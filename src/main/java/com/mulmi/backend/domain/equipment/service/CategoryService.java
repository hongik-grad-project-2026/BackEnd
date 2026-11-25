package com.mulmi.backend.domain.equipment.service;

import com.mulmi.backend.domain.equipment.dto.request.CategoryCreateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.request.CategoryUpdateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.response.CategoryResponseDTO;

import java.util.List;

public interface CategoryService {

    // 카테고리 등록
    CategoryResponseDTO createCategory(CategoryCreateRequestDTO dto);

    // 카테고리 목록 조회 (학생이면 졸업전시 경품 카테고리를 뺀다)
    List<CategoryResponseDTO> getCategories(boolean student);

    // 카테고리 수정
    CategoryResponseDTO updateCategory(Long categoryId, CategoryUpdateRequestDTO dto);

    // 카테고리 삭제
    void deleteCategory(Long categoryId);

}
