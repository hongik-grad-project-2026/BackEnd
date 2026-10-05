package com.mulmi.backend.domain.equipment.dto.response;

public record EquipmentModelCreateResponseDTO(
        Long modelId,
        Integer createdItemCount,
        String firstLabelCode,
        String lastLabelCode
) {
}
