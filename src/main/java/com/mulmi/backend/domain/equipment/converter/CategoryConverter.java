package com.mulmi.backend.domain.equipment.converter;

import com.mulmi.backend.domain.equipment.dto.response.CategoryResponseDTO;
import com.mulmi.backend.domain.equipment.entity.Category;

public class CategoryConverter {

    // 이름 -> Category
    public static Category toCategory(String name) {
        return Category.builder()
                .name(name)
                .build();
    }

    // Category -> DTO
    public static CategoryResponseDTO toCategoryResponseDTO(Category category) {
        return new CategoryResponseDTO(
                category.getId(),
                category.getName()
        );
    }

}
