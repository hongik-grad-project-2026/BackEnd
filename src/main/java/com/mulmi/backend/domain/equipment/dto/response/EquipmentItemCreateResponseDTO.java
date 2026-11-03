package com.mulmi.backend.domain.equipment.dto.response;

// 기타 카테고리는 라벨이 없으므로 firstLabelCode, lastLabelCode가 null
public record EquipmentItemCreateResponseDTO(
        Long modelId,
        int createdItemCount,
        String firstLabelCode,
        String lastLabelCode
) {
}
