package com.mulmi.backend.domain.equipment.dto.response;

import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;

// 모델별 기기 목록의 한 줄
public record AdminEquipmentItemSummaryResponseDTO(
        Long itemId,
        String labelCode,
        EquipmentItemStatus status,
        String memo
) {
}
