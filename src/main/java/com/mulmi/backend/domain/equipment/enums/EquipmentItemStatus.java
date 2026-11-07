package com.mulmi.backend.domain.equipment.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum EquipmentItemStatus {
    AVAILABLE,
    RESERVED,   // 대여 예정 : 신청에 배정됨 (승인 대기 ~ 수령 대기)
    RENTED,
    BROKEN;

    // 대여 흐름에 묶여 있는 기기: 폐기 불가 (기기 폐기·모델 폐기)
    public static final Set<EquipmentItemStatus> IN_USE = Collections.unmodifiableSet(
            EnumSet.of(RESERVED, RENTED));
}
