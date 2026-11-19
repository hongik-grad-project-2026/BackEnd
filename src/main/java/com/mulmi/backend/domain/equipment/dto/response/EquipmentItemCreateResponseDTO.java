package com.mulmi.backend.domain.equipment.dto.response;

// 기타 카테고리는 접두사 없이 번호만 (예: firstLabelCode "001", lastLabelCode "005")
public record EquipmentItemCreateResponseDTO(
        Long modelId,
        int createdItemCount,
        String firstLabelCode,
        String lastLabelCode
) {
}
