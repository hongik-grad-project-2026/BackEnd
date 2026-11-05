package com.mulmi.backend.domain.rental.enums;

public enum RentalStatus {
    PENDING_APPROVAL,   // 신청 성공, 기기 배정됨(RESERVED), 조교 승인 대기
    READY_FOR_PICKUP,   // 승인됨, 수령 대기
    RENTING,            // 수령해서 대여 중
    OVERDUE,            // 반납 마감 지남
    RETURNED,
    REJECTED,
    CANCELED,
    NO_SHOW             // 수령 마감까지 안 옴 → 기기는 AVAILABLE로 복귀
}
