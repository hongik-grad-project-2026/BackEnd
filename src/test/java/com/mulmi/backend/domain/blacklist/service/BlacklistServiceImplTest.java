package com.mulmi.backend.domain.blacklist.service;

import com.mulmi.backend.domain.blacklist.dto.request.RegisterBlacklistRequestDTO;
import com.mulmi.backend.domain.blacklist.dto.response.BlacklistPageResponseDTO;
import com.mulmi.backend.domain.blacklist.dto.response.BlacklistResponseDTO;
import com.mulmi.backend.domain.blacklist.entity.Blacklist;
import com.mulmi.backend.domain.blacklist.enums.BlacklistReasonType;
import com.mulmi.backend.domain.blacklist.exception.BlacklistException;
import com.mulmi.backend.domain.blacklist.exception.code.BlacklistErrorCode;
import com.mulmi.backend.domain.blacklist.repository.BlacklistRepository;
import com.mulmi.backend.domain.equipment.entity.Category;
import com.mulmi.backend.domain.equipment.entity.EquipmentItem;
import com.mulmi.backend.domain.equipment.entity.EquipmentModel;
import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;
import com.mulmi.backend.domain.rental.entity.Rental;
import com.mulmi.backend.domain.rental.enums.ApplicationType;
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
import com.mulmi.backend.domain.user.enums.UserStatus;
import com.mulmi.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlacklistServiceImplTest {

    @Mock
    private BlacklistRepository blacklistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private RentalStockCounter rentalStockCounter;

    @InjectMocks
    private BlacklistServiceImpl blacklistService;

    @Test
    void registerCreatesManualSemesterRestriction() {
        User student = user(1L, UserRole.STUDENT);
        User administrator = user(2L, UserRole.ASSISTANT);
        Semester semester = semester();
        given(userRepository.findWithLockById(1L)).willReturn(Optional.of(student));
        given(userRepository.findById(2L)).willReturn(Optional.of(administrator));
        given(semesterRepository.findById(10L)).willReturn(Optional.of(semester));
        given(blacklistRepository.save(org.mockito.ArgumentMatchers.any(Blacklist.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        BlacklistResponseDTO result = blacklistService.register(
                2L,
                1L,
                new RegisterBlacklistRequestDTO(10L, "  관리자 확인 사유  ")
        );

        ArgumentCaptor<Blacklist> captor = ArgumentCaptor.forClass(Blacklist.class);
        verify(blacklistRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(student);
        assertThat(captor.getValue().getSemester()).isSameAs(semester);
        assertThat(captor.getValue().getRegisteredBy()).isSameAs(administrator);
        assertThat(captor.getValue().getReasonType()).isEqualTo(BlacklistReasonType.MANUAL);
        assertThat(captor.getValue().getReasonDetail()).isEqualTo("관리자 확인 사유");
        assertThat(result.active()).isTrue();
    }

    @Test
    void registerRejectsDuplicateActiveRestrictionInSameSemester() {
        given(userRepository.findWithLockById(1L)).willReturn(Optional.of(user(1L, UserRole.STUDENT)));
        given(userRepository.findById(2L)).willReturn(Optional.of(user(2L, UserRole.ASSISTANT)));
        given(semesterRepository.findById(10L)).willReturn(Optional.of(semester()));
        given(blacklistRepository.existsByUserIdAndSemesterIdAndReleasedAtIsNull(1L, 10L))
                .willReturn(true);

        assertThatThrownBy(() -> blacklistService.register(
                2L,
                1L,
                new RegisterBlacklistRequestDTO(10L, "중복 등록")
        ))
                .isInstanceOf(BlacklistException.class)
                .extracting("code")
                .isEqualTo(BlacklistErrorCode.ACTIVE_BLACKLIST_ALREADY_EXISTS);
    }

    @Test
    void getHistoryReturnsReleasedRecordsWithoutDeletingThem() {
        User student = user(1L, UserRole.STUDENT);
        Blacklist blacklist = Blacklist.builder()
                .id(100L)
                .user(student)
                .semester(semester())
                .reasonType(BlacklistReasonType.NO_SHOW)
                .build();
        PageRequest pageable = PageRequest.of(0, 20);
        given(userRepository.findById(1L)).willReturn(Optional.of(student));
        given(blacklistRepository.findAllByUserIdOrderByIdDesc(1L, pageable))
                .willReturn(new PageImpl<>(List.of(blacklist), pageable, 1));

        BlacklistPageResponseDTO result = blacklistService.getHistory(1L, 0, 20);

        assertThat(result.blacklists()).hasSize(1);
        assertThat(result.blacklists().get(0).blacklistId()).isEqualTo(100L);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void releaseRecordsAdministratorReasonAndTime() {
        User student = user(1L, UserRole.STUDENT);
        User administrator = user(2L, UserRole.ASSISTANT);
        Blacklist blacklist = Blacklist.builder()
                .id(100L)
                .user(student)
                .semester(semester())
                .reasonType(BlacklistReasonType.MANUAL)
                .build();
        given(userRepository.findById(2L)).willReturn(Optional.of(administrator));
        given(blacklistRepository.findWithLockById(100L)).willReturn(Optional.of(blacklist));

        BlacklistResponseDTO result = blacklistService.release(2L, 100L, "  소명 확인  ");

        assertThat(blacklist.isActive()).isFalse();
        assertThat(blacklist.getReleasedBy()).isSameAs(administrator);
        assertThat(blacklist.getReleaseReason()).isEqualTo("소명 확인");
        assertThat(blacklist.getReleasedAt()).isNotNull();
        assertThat(result.active()).isFalse();
    }

    @Test
    void releaseRejectsAlreadyReleasedRecord() {
        User administrator = user(2L, UserRole.ASSISTANT);
        Blacklist blacklist = Blacklist.builder()
                .id(100L)
                .user(user(1L, UserRole.STUDENT))
                .semester(semester())
                .reasonType(BlacklistReasonType.MANUAL)
                .build();
        blacklist.release(administrator, "이미 해제", java.time.LocalDateTime.now());
        given(userRepository.findById(2L)).willReturn(Optional.of(administrator));
        given(blacklistRepository.findWithLockById(100L)).willReturn(Optional.of(blacklist));

        assertThatThrownBy(() -> blacklistService.release(2L, 100L, "다시 해제"))
                .isInstanceOf(BlacklistException.class)
                .extracting("code")
                .isEqualTo(BlacklistErrorCode.BLACKLIST_ALREADY_RELEASED);
    }

    @Test
    void validateRentalEligibilityRejectsActiveSemesterRestriction() {
        given(blacklistRepository.existsByUserIdAndSemesterIdAndReleasedAtIsNull(1L, 10L))
                .willReturn(true);

        assertThatThrownBy(() -> blacklistService.validateRentalEligibility(1L, 10L))
                .isInstanceOf(RentalException.class)
                .extracting("code")
                .isEqualTo(RentalErrorCode.BLACKLISTED_USER);
    }

    @Test
    void registerNoShowCreatesRestrictionAndCancelsOtherUncollectedRentals() {
        Semester semester = semester();
        User student = user(1L, UserRole.STUDENT);
        EquipmentItem sourceItem = item(100L, EquipmentItemStatus.AVAILABLE);
        Rental noShow = rental(200L, student, semester, sourceItem, RentalStatus.NO_SHOW);
        EquipmentItem reservedItem = item(101L, EquipmentItemStatus.RESERVED);
        Rental pending = rental(201L, student, semester, reservedItem, RentalStatus.PENDING_APPROVAL);
        LocalDateTime now = LocalDateTime.of(2026, 9, 15, 20, 1);
        given(userRepository.findWithLockById(1L)).willReturn(Optional.of(student));
        given(rentalRepository.findAllByUserIdAndSemesterIdAndStatusIn(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.anyCollection()
        )).willReturn(List.of(pending));

        blacklistService.registerNoShow(noShow, now);

        ArgumentCaptor<Blacklist> captor = ArgumentCaptor.forClass(Blacklist.class);
        verify(blacklistRepository).save(captor.capture());
        assertThat(captor.getValue().getReasonType()).isEqualTo(BlacklistReasonType.NO_SHOW);
        assertThat(captor.getValue().getSemester()).isSameAs(semester);
        assertThat(captor.getValue().getRental()).isSameAs(noShow);
        assertThat(pending.getStatus()).isEqualTo(RentalStatus.CANCELED);
        assertThat(pending.getCancelReason()).isEqualTo(CancelReasonType.BLACKLISTED);
        assertThat(reservedItem.getStatus()).isEqualTo(EquipmentItemStatus.AVAILABLE);
        verify(rentalStockCounter).addAfterCommit(50L, 1);
    }

    @Test
    void registerOverdueTargetsNextSemester() {
        Semester current = semester();
        Semester next = Semester.builder()
                .id(11L)
                .name("2027-1")
                .startDate(LocalDate.of(2027, 3, 1))
                .build();
        Rental overdue = rental(
                200L,
                user(1L, UserRole.STUDENT),
                current,
                item(100L, EquipmentItemStatus.AVAILABLE),
                RentalStatus.RETURNED
        );
        given(semesterRepository.findWithLockById(10L)).willReturn(Optional.of(current));
        given(semesterRepository.findNextWithLock(current.getStartDate()))
                .willReturn(Optional.of(next));
        given(userRepository.findWithLockById(1L)).willReturn(Optional.of(overdue.getUser()));

        blacklistService.registerOverdue(overdue, LocalDateTime.of(2027, 2, 1, 10, 0));

        ArgumentCaptor<Blacklist> captor = ArgumentCaptor.forClass(Blacklist.class);
        verify(blacklistRepository).save(captor.capture());
        assertThat(captor.getValue().getReasonType()).isEqualTo(BlacklistReasonType.OVERDUE);
        assertThat(captor.getValue().getSemester()).isSameAs(next);
    }

    private Semester semester() {
        return Semester.builder()
                .id(10L)
                .name("2026-2")
                .startDate(LocalDate.of(2026, 9, 1))
                .build();
    }

    private User user(Long id, UserRole role) {
        return User.builder()
                .id(id)
                .loginId(role == UserRole.STUDENT ? "C123456" : "assistant")
                .studentId(role == UserRole.STUDENT ? "C123456" : null)
                .password("encoded-password")
                .name(role == UserRole.STUDENT ? "학생" : "조교")
                .email(role == UserRole.STUDENT ? "student@example.com" : "assistant@example.com")
                .phoneNumber("01012345678")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private EquipmentItem item(Long id, EquipmentItemStatus status) {
        EquipmentModel model = EquipmentModel.builder()
                .id(50L)
                .name("테스트 모델")
                .category(Category.builder().id(5L).name("노트북").build())
                .build();
        return EquipmentItem.builder()
                .id(id)
                .equipmentModel(model)
                .labelCode("M-" + id)
                .status(status)
                .build();
    }

    private Rental rental(
            Long id,
            User student,
            Semester semester,
            EquipmentItem item,
            RentalStatus status
    ) {
        return Rental.builder()
                .id(id)
                .user(student)
                .equipmentModel(item.getEquipmentModel())
                .equipmentItem(item)
                .semester(semester)
                .applicationType(ApplicationType.ON_DEMAND)
                .purpose("테스트")
                .status(status)
                .expectedPickupDate(LocalDate.of(2026, 9, 10))
                .build();
    }
}
