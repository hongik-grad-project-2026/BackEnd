package com.mulmi.backend.domain.blacklist.service;

import com.mulmi.backend.domain.blacklist.dto.request.RegisterBlacklistRequestDTO;
import com.mulmi.backend.domain.blacklist.dto.response.BlacklistPageResponseDTO;
import com.mulmi.backend.domain.blacklist.dto.response.BlacklistResponseDTO;
import com.mulmi.backend.domain.blacklist.entity.Blacklist;
import com.mulmi.backend.domain.blacklist.enums.BlacklistReasonType;
import com.mulmi.backend.domain.blacklist.exception.BlacklistException;
import com.mulmi.backend.domain.blacklist.exception.code.BlacklistErrorCode;
import com.mulmi.backend.domain.blacklist.repository.BlacklistRepository;
import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;
import com.mulmi.backend.domain.rental.entity.Rental;
import com.mulmi.backend.domain.rental.enums.CancelReasonType;
import com.mulmi.backend.domain.rental.enums.RentalStatus;
import com.mulmi.backend.domain.rental.exception.RentalException;
import com.mulmi.backend.domain.rental.exception.code.RentalErrorCode;
import com.mulmi.backend.domain.rental.repository.RentalRepository;
import com.mulmi.backend.domain.rental.stock.RentalStockCounter;
import com.mulmi.backend.domain.semester.entity.Semester;
import com.mulmi.backend.domain.semester.repository.SemesterRepository;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.domain.user.enums.UserRole;
import com.mulmi.backend.domain.user.exception.UserException;
import com.mulmi.backend.domain.user.exception.code.UserErrorCode;
import com.mulmi.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlacklistServiceImpl implements BlacklistService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private final BlacklistRepository blacklistRepository;
    private final UserRepository userRepository;
    private final SemesterRepository semesterRepository;
    private final RentalRepository rentalRepository;
    private final RentalStockCounter rentalStockCounter;

    @Override
    @Transactional
    public BlacklistResponseDTO register(
            Long administratorId,
            Long userId,
            RegisterBlacklistRequestDTO request
    ) {
        User student = findStudentWithLock(userId);
        User administrator = findAdministrator(administratorId);
        Semester semester = semesterRepository.findById(request.semesterId())
                .orElseThrow(() -> new BlacklistException(BlacklistErrorCode.SEMESTER_NOT_FOUND));

        if (blacklistRepository.existsByUserIdAndSemesterIdAndReleasedAtIsNull(userId, semester.getId())) {
            throw new BlacklistException(BlacklistErrorCode.ACTIVE_BLACKLIST_ALREADY_EXISTS);
        }

        Blacklist blacklist = blacklistRepository.save(Blacklist.builder()
                .user(student)
                .semester(semester)
                .reasonType(BlacklistReasonType.MANUAL)
                .reasonDetail(request.reasonDetail().trim())
                .registeredBy(administrator)
                .build());
        cancelUncollectedRentals(student.getId(), semester.getId(), LocalDateTime.now(KOREA));
        return BlacklistResponseDTO.from(blacklist);
    }

    @Override
    public BlacklistPageResponseDTO getHistory(Long userId, int page, int size) {
        findStudent(userId);
        Page<BlacklistResponseDTO> history = blacklistRepository
                .findAllByUserIdOrderByIdDesc(userId, PageRequest.of(page, size))
                .map(BlacklistResponseDTO::from);
        return BlacklistPageResponseDTO.from(history);
    }

    @Override
    @Transactional
    public BlacklistResponseDTO release(Long administratorId, Long blacklistId, String releaseReason) {
        User administrator = findAdministrator(administratorId);
        Blacklist blacklist = blacklistRepository.findById(blacklistId)
                .orElseThrow(() -> new BlacklistException(BlacklistErrorCode.BLACKLIST_NOT_FOUND));
        if (!blacklist.isActive()) {
            throw new BlacklistException(BlacklistErrorCode.BLACKLIST_ALREADY_RELEASED);
        }

        blacklist.release(administrator, releaseReason.trim(), LocalDateTime.now(KOREA));
        return BlacklistResponseDTO.from(blacklist);
    }

    @Override
    public void validateRentalEligibility(Long userId, Long semesterId) {
        if (blacklistRepository.existsByUserIdAndSemesterIdAndReleasedAtIsNull(userId, semesterId)) {
            throw new RentalException(RentalErrorCode.BLACKLISTED_USER);
        }
    }

    @Override
    @Transactional
    public void registerNoShow(Rental rental, LocalDateTime now) {
        registerAutomatic(rental, rental.getSemester(), BlacklistReasonType.NO_SHOW, now);
    }

    @Override
    @Transactional
    public void registerOverdue(Rental rental, LocalDateTime now) {
        Semester nextSemester = semesterRepository
                .findFirstByStartDateGreaterThanOrderByStartDateAsc(rental.getSemester().getStartDate())
                .orElseGet(() -> createNextSemester(rental.getSemester()));
        registerAutomatic(rental, nextSemester, BlacklistReasonType.OVERDUE, now);
    }

    private void registerAutomatic(
            Rental rental,
            Semester targetSemester,
            BlacklistReasonType reasonType,
            LocalDateTime now
    ) {
        // 신청과 동시에 자동 제재가 생겨도 신청 쪽이 커밋 후 최신 제재 상태를 보도록
        // 수동 등록과 같은 사용자 행 잠금을 사용한다.
        User student = findStudentWithLock(rental.getUser().getId());
        if (blacklistRepository.existsByRentalIdAndReasonType(rental.getId(), reasonType)) {
            return;
        }
        blacklistRepository.save(Blacklist.builder()
                .user(student)
                .semester(targetSemester)
                .rental(rental)
                .reasonType(reasonType)
                .build());
        cancelUncollectedRentals(rental.getUser().getId(), targetSemester.getId(), now);
    }

    private void cancelUncollectedRentals(Long userId, Long semesterId, LocalDateTime now) {
        Set<RentalStatus> cancellable = Set.of(
                RentalStatus.PENDING_APPROVAL,
                RentalStatus.READY_FOR_PICKUP
        );
        for (Rental pending : rentalRepository.findAllByUserIdAndSemesterIdAndStatusIn(
                userId,
                semesterId,
                cancellable
        )) {
            pending.getEquipmentItem().changeStatus(EquipmentItemStatus.AVAILABLE);
            pending.cancel(CancelReasonType.BLACKLISTED, now);
            rentalStockCounter.addAfterCommit(pending.getEquipmentModel().getId(), 1);
        }
    }

    private Semester createNextSemester(Semester current) {
        java.time.LocalDate startDate = current.defaultNextStartDate();
        int term = startDate.getMonthValue() <= 6 ? 1 : 2;
        return semesterRepository.save(Semester.builder()
                .name(startDate.getYear() + "-" + term)
                .startDate(startDate)
                .build());
    }

    private User findStudent(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        if (user.getRole() != UserRole.STUDENT) {
            throw new UserException(UserErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private User findStudentWithLock(Long userId) {
        User user = userRepository.findWithLockById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        if (user.getRole() != UserRole.STUDENT) {
            throw new UserException(UserErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private User findAdministrator(Long administratorId) {
        User user = userRepository.findById(administratorId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        if (user.getRole() != UserRole.ASSISTANT) {
            throw new UserException(UserErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
