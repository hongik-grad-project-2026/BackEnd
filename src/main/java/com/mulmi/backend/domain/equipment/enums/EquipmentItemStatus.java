package com.mulmi.backend.domain.equipment.enums;

public enum EquipmentItemStatus {
    AVAILABLE,
    RESERVED,   // 대여 예정 : 신청에 배정됨 (승인 대기 ~ 수령 대기)
    RENTED,
    BROKEN
}
