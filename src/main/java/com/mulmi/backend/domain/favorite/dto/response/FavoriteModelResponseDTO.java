package com.mulmi.backend.domain.favorite.dto.response;

import com.mulmi.backend.domain.equipment.enums.EquipmentModelStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FavoriteModelResponseDTO(
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
        LocalDate disposalDate,
        LocalDateTime favoritedAt
) {
}
