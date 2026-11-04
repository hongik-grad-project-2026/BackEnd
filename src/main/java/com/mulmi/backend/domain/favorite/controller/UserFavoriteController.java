package com.mulmi.backend.domain.favorite.controller;

import com.mulmi.backend.domain.favorite.dto.response.FavoritePageResponseDTO;
import com.mulmi.backend.domain.favorite.exception.code.FavoriteSuccessCode;
import com.mulmi.backend.domain.favorite.service.FavoriteService;
import com.mulmi.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/users/me/favorites")
@Tag(name = "찜", description = "학생 기자재 모델 찜 API")
public class UserFavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "내 찜 목록 조회")
    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<FavoritePageResponseDTO> getMyFavorites(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.onSuccess(
                FavoriteSuccessCode.FAVORITES_FOUND,
                favoriteService.getMyFavorites(userId, page, size)
        );
    }
}
