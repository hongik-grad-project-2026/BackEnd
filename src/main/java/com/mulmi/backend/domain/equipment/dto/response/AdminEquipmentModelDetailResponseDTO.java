package com.mulmi.backend.domain.equipment.dto.response;

// 기기 목록 페이지 머리말 : 모델명 + 라벨 접두사 + 다음 라벨 번호 (접두사 없는 모델은 둘 다 null)
public record AdminEquipmentModelDetailResponseDTO(
        Long modelId,
        String name,
        String labelPrefix,
        Integer nextLabelNumber
) {
}
