package com.mulmi.backend.domain.blacklist.entity;

import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.domain.user.enums.UserRole;
import com.mulmi.backend.domain.user.enums.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BlacklistTest {

    @Test
    void updatePeriodAndReasonChangesBlacklistDetails() {
        Blacklist blacklist = createBlacklist(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        blacklist.updatePeriodAndReason(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 15),
                "반납 지연"
        );

        assertThat(blacklist.getStartDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(blacklist.getEndDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(blacklist.getReason()).isEqualTo("반납 지연");
    }

    @Test
    void overlapsIncludesBoundaryDates() {
        Blacklist blacklist = createBlacklist(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        assertThat(blacklist.overlaps(
                LocalDate.of(2026, 9, 30),
                LocalDate.of(2026, 10, 10)
        )).isTrue();
        assertThat(blacklist.overlaps(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 10)
        )).isFalse();
    }

    private Blacklist createBlacklist(LocalDate startDate, LocalDate endDate) {
        User user = User.builder()
                .id(1L)
                .loginId("C123456")
                .studentId("C123456")
                .password("encoded-password")
                .name("홍길동")
                .email("hong@example.com")
                .phoneNumber("01012345678")
                .college("공과대학")
                .department("컴퓨터공학과")
                .role(UserRole.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();

        return Blacklist.builder()
                .id(1L)
                .user(user)
                .rentalId(101L)
                .startDate(startDate)
                .endDate(endDate)
                .reason("장기 연체")
                .build();
    }
}
