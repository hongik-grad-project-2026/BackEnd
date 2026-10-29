package com.mulmi.backend.domain.equipment.enums;

public enum EquipmentItemStatus {
    AVAILABLE,
    RESERVED,   // 대여 예정 : 승인된 신청에 배정되어 수령 대기
    RENTED,
    BROKEN
}
