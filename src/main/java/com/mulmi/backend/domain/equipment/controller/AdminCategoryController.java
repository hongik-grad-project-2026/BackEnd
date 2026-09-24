package com.mulmi.backend.domain.equipment.controller;

import com.mulmi.backend.domain.equipment.dto.request.CategoryCreateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.request.CategoryUpdateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.response.CategoryResponseDTO;
import com.mulmi.backend.domain.equipment.exception.code.CategorySuccessCode;
import com.mulmi.backend.domain.equipment.service.CategoryService;
import com.mulmi.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/categories")
@Tag(name = "카테고리 관리", description = "조교용 카테고리 관리 API")
public class AdminCategoryController {

    private final CategoryService categoryService;

    // 카테고리 등록
    @Operation(summary = "카테고리 등록")
    @PostMapping
    @PreAuthorize("hasRole('ASSISTANT')")
    public ApiResponse<CategoryResponseDTO> createCategory(
            @RequestBody @Valid CategoryCreateRequestDTO dto
    ) {
        return ApiResponse.onSuccess(
                CategorySuccessCode.NEW_CATEGORY,
                categoryService.createCategory(dto)
        );
    }

    // 카테고리 수정
    @Operation(summary = "카테고리 수정")
    @PatchMapping("/{categoryId}")
    @PreAuthorize("hasRole('ASSISTANT')")
    public ApiResponse<CategoryResponseDTO> updateCategory(
            @PathVariable Long categoryId,
            @RequestBody @Valid CategoryUpdateRequestDTO dto
    ) {
        return ApiResponse.onSuccess(
                CategorySuccessCode.UPDATED,
                categoryService.updateCategory(categoryId, dto)
        );
    }

}
