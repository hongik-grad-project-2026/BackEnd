package com.mulmi.backend.domain.rental.enums;

// 반려 사유 (명세 53번, ERD reject_reason_type)
public enum RejectReasonType {
    INSUFFICIENT_STOCK, // 기기 재고 부족
    RENTAL_SUSPENDED,   // 대여 중지된 기기 (기기 고장 등)
    OTHER               // 기타 : 상세 사유 필수
}
