package com.mulmi.backend.domain.equipment.dto.response;

import com.mulmi.backend.domain.equipment.enums.EquipmentModelStatus;

import java.time.LocalDate;

// 관리자 모델 목록의 한 줄
public record AdminEquipmentModelSummaryResponseDTO(
        Long modelId,
        String modelImageUrl,
        String name,
        Long categoryId,
        String categoryName,
        String components,
        String specification,
        String description,
        long availableCount,
        long totalCount,
        EquipmentModelStatus rentalStatus,
        LocalDate receivedDate,
        LocalDate disposalDate
) {
}
