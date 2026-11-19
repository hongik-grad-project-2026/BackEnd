package com.mulmi.backend.domain.blacklist.service;

import com.mulmi.backend.domain.blacklist.dto.request.RegisterBlacklistRequestDTO;
import com.mulmi.backend.domain.blacklist.dto.response.BlacklistPageResponseDTO;
import com.mulmi.backend.domain.blacklist.dto.response.BlacklistResponseDTO;
import com.mulmi.backend.domain.blacklist.entity.Blacklist;
import com.mulmi.backend.domain.blacklist.enums.BlacklistReasonType;
import com.mulmi.backend.domain.blacklist.exception.BlacklistException;
import com.mulmi.backend.domain.blacklist.exception.code.BlacklistErrorCode;
import com.mulmi.backend.domain.blacklist.repository.BlacklistRepository;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlacklistServiceImpl implements BlacklistService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private final BlacklistRepository blacklistRepository;
    private final UserRepository userRepository;
    private final SemesterRepository semesterRepository;

    @Override
    @Transactional
    public BlacklistResponseDTO register(
            Long administratorId,
            Long userId,
            RegisterBlacklistRequestDTO request
    ) {
        User student = findStudent(userId);
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

    private User findStudent(Long userId) {
        User user = userRepository.findById(userId)
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
