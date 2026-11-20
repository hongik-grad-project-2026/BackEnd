package com.mulmi.backend.domain.equipment.dto.response;

import java.util.List;

public record DashboardCategoryUsageResponseDTO (
        String categoryName,
        List<DashboardModelUsageResponseDTO> models
){
}
