package com.mulmi.backend.domain.equipment.dto.response;

// 모든 카테고리가 섞여 나오므로 categoryName을 함께 준다
public record DashboardIdleModelResponseDTO(
        String modelName,
        String categoryName,
        long idleDays
) {
}
