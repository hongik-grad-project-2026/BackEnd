package com.mulmi.backend.domain.equipment.controller;

import com.mulmi.backend.domain.equipment.dto.response.CategoryResponseDTO;
import com.mulmi.backend.domain.equipment.exception.code.CategorySuccessCode;
import com.mulmi.backend.domain.equipment.service.CategoryService;
import com.mulmi.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
@Tag(name = "카테고리 조회", description = "카테고리 조회 API")
public class CategoryController {
    private final CategoryService categoryService;

    // 카테고리 조회
    // API 명세 21번 : 조회는 조교, 근로생, 학생 모두 가능하므로 로그인만 되어있다면 별도의 권한검사 안함
    @Operation(summary = "카테고리 조회")
    @GetMapping
    public ApiResponse<List<CategoryResponseDTO>> getCategories(Authentication authentication) {
        return ApiResponse.onSuccess(
                CategorySuccessCode.CATEGORIES_FOUND,
                categoryService.getCategories(isStudent(authentication))
        );
    }

    // 로그인 필터가 토큰에서 꺼내 SecurityContext에 넣어 둔 권한 목록으로 학생인지 본다
    static boolean isStudent(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_STUDENT".equals(authority.getAuthority()));
    }

}
