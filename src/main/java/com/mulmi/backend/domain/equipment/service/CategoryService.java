package com.mulmi.backend.domain.equipment.service;

import com.mulmi.backend.domain.equipment.dto.request.CategoryCreateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.request.CategoryUpdateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.response.CategoryResponseDTO;

import java.util.List;

public interface CategoryService {

    // 카테고리 등록
    CategoryResponseDTO createCategory(CategoryCreateRequestDTO dto);

    // 카테고리 목록 조회
    List<CategoryResponseDTO> getCategories();

    // 카테고리 수정
    CategoryResponseDTO updateCategory(Long categoryId, CategoryUpdateRequestDTO dto);

    // 카테고리 삭제는 EquipmentModel 엔티티가 생긴 뒤에 추가한다.
}
