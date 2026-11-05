package com.mulmi.backend.domain.semester.entity;

import com.mulmi.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "semester")
public class Semester extends BaseEntity {

    // 사전신청 1주 → 수령 1주 → 상시 (11-05 확정)
    private static final int PRE_APPLY_DAYS = 7;
    private static final int PICKUP_DAYS = 7;
    // 상시 수령희망일: 오늘부터 3일 (11-05 확정)
    private static final int ON_DEMAND_PICKUP_DAYS = 3;
    // 멀티미디어실 운영 종료 시각: 수령·반납 마감
    private static final LocalTime CLOSING_TIME = LocalTime.of(20, 0);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "semester_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    // 개강일 = 사전신청 시작일
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    // 종강일 (화면 표시용 — 대여는 방학까지 이어지므로 반납 계산에는 쓰지 않는다)
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // 경계는 모두 [이상, 미만) — 23:59:59.999 같은 끝값을 다루지 않기 위해
    private LocalDateTime preApplyStart() {
        return startDate.atStartOfDay();
    }

    private LocalDateTime pickupStart() {
        return startDate.plusDays(PRE_APPLY_DAYS).atStartOfDay();
    }

    private LocalDateTime onDemandStart() {
        return startDate.plusDays(PRE_APPLY_DAYS + PICKUP_DAYS).atStartOfDay();
    }

    // now는 호출하는 쪽이 한국 시간으로 만들어 넘긴다 (서버 컨테이너는 UTC)
    public boolean isPreApplyPeriod(LocalDateTime now) {
        return !now.isBefore(preApplyStart()) && now.isBefore(pickupStart());
    }

    // 끝은 다음 학기 개강 전날까지(방학 포함). 이 메서드는 끝을 보지 않는다 —
    // '지금 학기' = 개강일이 오늘 이전인 학기 중 가장 최근 것으로 고르면 다음 학기가 시작되는 순간 자동으로 넘어간다
    public boolean isOnDemandPeriod(LocalDateTime now) {
        return !now.isBefore(onDemandStart());
    }

    // 사전신청 수령희망일: 수령 기간의 날짜(예: 3/8 ~ 3/14)만 고를 수 있다
    public boolean isPickupDateSelectable(LocalDate date) {
        LocalDate first = pickupStart().toLocalDate();
        LocalDate last = first.plusDays(PICKUP_DAYS - 1);
        return !date.isBefore(first) && !date.isAfter(last);
    }

    // 상시 수령희망일: 오늘부터 3일(오늘·내일·모레), 단 20:00 이후엔 오늘 제외
    // 다음 학기 개강일을 넘는지는 이 엔티티가 모르므로 서비스에서 검사한다
    public boolean isOnDemandPickupDateSelectable(LocalDate date, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalDate first = now.toLocalTime().isBefore(CLOSING_TIME) ? today : today.plusDays(1);
        LocalDate last = today.plusDays(ON_DEMAND_PICKUP_DAYS - 1);
        return !date.isBefore(first) && !date.isAfter(last);
    }

    // 사전신청 수령 마감: 수령 기간 마지막 날 20:00 (지나면 노쇼)
    public LocalDateTime pickupDeadline() {
        return pickupStart().toLocalDate().plusDays(PICKUP_DAYS - 1).atTime(CLOSING_TIME);
    }

    // 이 학기 대여의 최종 반납 마감: 다음 학기 개강 전날 20:00 (방학 포함, 11-05 확정)
    // 장기 대여의 반납일이고, 단기 대여(수령일+N일)도 이 시각을 넘을 수 없다
    public static LocalDateTime returnDueAt(LocalDate nextSemesterStart) {
        return nextSemesterStart.minusDays(1).atTime(CLOSING_TIME);
    }

    // 다음 학기가 아직 등록되지 않았을 때 쓰는 개강일: 회의록의 고정 개강일(3/1, 9/1)
    public LocalDate defaultNextStartDate() {
        return startDate.getMonthValue() <= 6
                ? LocalDate.of(startDate.getYear(), 9, 1)
                : LocalDate.of(startDate.getYear() + 1, 3, 1);
    }
}
