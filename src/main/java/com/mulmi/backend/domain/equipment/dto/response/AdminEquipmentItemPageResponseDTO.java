package com.mulmi.backend.domain.equipment.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public record AdminEquipmentItemPageResponseDTO(
        List<AdminEquipmentItemSummaryResponseDTO> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        Map<String, Long> statusCounts
) {
    public static AdminEquipmentItemPageResponseDTO of(
            Page<AdminEquipmentItemSummaryResponseDTO> page,
            Map<String, Long> statusCounts
    ) {
        return new AdminEquipmentItemPageResponseDTO(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                statusCounts
        );
    }
}
