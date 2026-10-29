package com.mulmi.backend.domain.equipment.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public record AdminEquipmentModelPageResponseDTO(
        List<AdminEquipmentModelSummaryResponseDTO> models,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        Map<String, Long> categoryCounts
) {
    public static AdminEquipmentModelPageResponseDTO of(
            Page<AdminEquipmentModelSummaryResponseDTO> page,
            Map<String, Long> categoryCounts
    ) {
        return new AdminEquipmentModelPageResponseDTO(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                categoryCounts
        );
    }
}
