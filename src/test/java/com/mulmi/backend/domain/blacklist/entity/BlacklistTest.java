package com.mulmi.backend.domain.blacklist.entity;

import com.mulmi.backend.domain.blacklist.enums.BlacklistReasonType;
import com.mulmi.backend.domain.semester.entity.Semester;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.domain.user.enums.UserRole;
import com.mulmi.backend.domain.user.enums.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BlacklistTest {

    @Test
    void updateChangesTargetSemesterAndReasonDetail() {
        Semester firstSemester = semester(1L, "2026-1", LocalDate.of(2026, 3, 1));
        Semester secondSemester = semester(2L, "2026-2", LocalDate.of(2026, 9, 1));
        Blacklist blacklist = blacklist(firstSemester);

        blacklist.update(secondSemester, "학생 사정 확인 후 제재 학기 변경");

        assertThat(blacklist.getSemester()).isSameAs(secondSemester);
        assertThat(blacklist.getReasonDetail()).isEqualTo("학생 사정 확인 후 제재 학기 변경");
    }

    @Test
    void releasePreservesHistoryAndMakesBlacklistInactive() {
        User administrator = user(2L, UserRole.ASSISTANT);
        LocalDateTime releasedAt = LocalDateTime.of(2026, 11, 19, 10, 30);
        Blacklist blacklist = blacklist(semester(2L, "2026-2", LocalDate.of(2026, 9, 1)));

        blacklist.release(administrator, "소명 내용 확인", releasedAt);

        assertThat(blacklist.isActive()).isFalse();
        assertThat(blacklist.getReleasedBy()).isSameAs(administrator);
        assertThat(blacklist.getReleaseReason()).isEqualTo("소명 내용 확인");
        assertThat(blacklist.getReleasedAt()).isEqualTo(releasedAt);
    }

    private Blacklist blacklist(Semester semester) {
        return Blacklist.builder()
                .id(1L)
                .user(user(1L, UserRole.STUDENT))
                .semester(semester)
                .reasonType(BlacklistReasonType.MANUAL)
                .reasonDetail("관리자 직접 등록")
                .build();
    }

    private Semester semester(Long id, String name, LocalDate startDate) {
        return Semester.builder()
                .id(id)
                .name(name)
                .startDate(startDate)
                .build();
    }

    private User user(Long id, UserRole role) {
        return User.builder()
                .id(id)
                .loginId(role == UserRole.STUDENT ? "C123456" : "assistant")
                .studentId(role == UserRole.STUDENT ? "C123456" : null)
                .password("encoded-password")
                .name(role == UserRole.STUDENT ? "홍길동" : "테스트조교")
                .email(role == UserRole.STUDENT ? "hong@example.com" : "assistant@example.com")
                .phoneNumber("01012345678")
                .college("공과대학")
                .department("컴퓨터공학과")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
