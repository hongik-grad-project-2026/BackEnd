package com.mulmi.backend.domain.semester.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SemesterTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 26);

    // 상시 수령희망일 첫날 : 20:00 전이면 오늘, 20:00부터 내일 (간식 수령희망일도 이 값)
    @Test
    void 상시_수령희망일_첫날은_20시부터_내일이다() {
        assertThat(Semester.firstOnDemandPickupDate(TODAY.atTime(19, 59, 59))).isEqualTo(TODAY);
        assertThat(Semester.firstOnDemandPickupDate(TODAY.atTime(20, 0))).isEqualTo(TODAY.plusDays(1));
        assertThat(Semester.firstOnDemandPickupDate(TODAY.atTime(23, 59))).isEqualTo(TODAY.plusDays(1));
    }

    // 첫날을 꺼낸 뒤에도 고를 수 있는 범위(첫날 ~ 오늘+2일)는 그대로다
    @Test
    void 상시_수령희망일_범위는_그대로다() {
        Semester semester = Semester.builder().startDate(TODAY.minusDays(30)).build();
        LocalDateTime evening = TODAY.atTime(20, 30);

        assertThat(semester.isOnDemandPickupDateSelectable(TODAY, evening)).isFalse();
        assertThat(semester.isOnDemandPickupDateSelectable(TODAY.plusDays(1), evening)).isTrue();
        assertThat(semester.isOnDemandPickupDateSelectable(TODAY.plusDays(2), evening)).isTrue();
        assertThat(semester.isOnDemandPickupDateSelectable(TODAY.plusDays(3), evening)).isFalse();
        assertThat(semester.isOnDemandPickupDateSelectable(TODAY, TODAY.atTime(10, 0))).isTrue();
    }
}
