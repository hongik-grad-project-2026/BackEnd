package com.mulmi.backend.domain.favorite.controller;

import com.mulmi.backend.domain.favorite.exception.code.FavoriteSuccessCode;
import com.mulmi.backend.domain.favorite.service.FavoriteService;
import com.mulmi.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/equipment-models/{model_id}/favorites")
@Tag(name = "찜", description = "학생 기자재 모델 찜 API")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "기자재 모델 찜 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Void> addFavorite(
            @AuthenticationPrincipal Long userId,
            @PathVariable("model_id") Long modelId
    ) {
        favoriteService.addFavorite(userId, modelId);
        return ApiResponse.onSuccess(FavoriteSuccessCode.FAVORITE_ADDED, null);
    }

    @Operation(summary = "기자재 모델 찜 취소")
    @DeleteMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Void> removeFavorite(
            @AuthenticationPrincipal Long userId,
            @PathVariable("model_id") Long modelId
    ) {
        favoriteService.removeFavorite(userId, modelId);
        return ApiResponse.onSuccess(FavoriteSuccessCode.FAVORITE_REMOVED, null);
    }
}
