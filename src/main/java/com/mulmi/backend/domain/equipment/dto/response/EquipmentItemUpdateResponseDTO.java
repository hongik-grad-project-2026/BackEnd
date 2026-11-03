package com.mulmi.backend.domain.equipment.dto.response;

import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;

public record EquipmentItemUpdateResponseDTO(
        Long itemId,
        String labelCode,
        EquipmentItemStatus status,
        String memo
) {
}
