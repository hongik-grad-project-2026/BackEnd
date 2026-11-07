package com.mulmi.backend.domain.rental.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum RentalStatus {
    PENDING_APPROVAL,   // 신청 성공, 기기 배정됨(RESERVED), 조교 승인 대기
    READY_FOR_PICKUP,   // 승인됨, 수령 대기
    RENTING,            // 수령해서 대여 중
    OVERDUE,            // 반납 마감 지남
    RETURNED,
    REJECTED,
    CANCELED,
    NO_SHOW;            // 수령 마감까지 안 옴 → 기기는 AVAILABLE로 복귀

    // 진행 중인 대여: 1인 1대에서 "빌리고 있음"으로 보는 상태 (명세 46 관문 3, 연체 포함 확정)
    public static final Set<RentalStatus> ACTIVE = Collections.unmodifiableSet(
            EnumSet.of(PENDING_APPROVAL, READY_FOR_PICKUP, RENTING, OVERDUE));
}
