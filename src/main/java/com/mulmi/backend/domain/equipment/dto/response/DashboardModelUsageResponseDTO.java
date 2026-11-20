package com.mulmi.backend.domain.equipment.dto.response;

public record DashboardModelUsageResponseDTO(
        String modelName,
        long rentedCount,
        long totalCount
){

}
