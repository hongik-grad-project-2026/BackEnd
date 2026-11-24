package com.mulmi.backend.domain.blacklist.controller;

import com.mulmi.backend.domain.blacklist.dto.request.RegisterBlacklistRequestDTO;
import com.mulmi.backend.domain.blacklist.dto.request.ReleaseBlacklistRequestDTO;
import com.mulmi.backend.domain.blacklist.dto.response.BlacklistPageResponseDTO;
import com.mulmi.backend.domain.blacklist.dto.response.BlacklistResponseDTO;
import com.mulmi.backend.domain.blacklist.exception.code.BlacklistSuccessCode;
import com.mulmi.backend.domain.blacklist.service.BlacklistService;
import com.mulmi.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@Tag(name = "블랙리스트 관리", description = "학생의 학기별 대여 제한 관리 API")
public class AdminBlacklistController {

    private final BlacklistService blacklistService;

    @Operation(summary = "학생 블랙리스트 수동 등록")
    @PostMapping("/users/{userId}/blacklists")
    @PreAuthorize("hasRole('ASSISTANT')")
    public ApiResponse<BlacklistResponseDTO> register(
            @AuthenticationPrincipal Long administratorId,
            @PathVariable Long userId,
            @RequestBody @Valid RegisterBlacklistRequestDTO request
    ) {
        return ApiResponse.onSuccess(
                BlacklistSuccessCode.BLACKLIST_REGISTERED,
                blacklistService.register(administratorId, userId, request)
        );
    }

    @Operation(summary = "학생 블랙리스트 이력 조회")
    @GetMapping("/users/{userId}/blacklists")
    @PreAuthorize("hasAnyRole('ASSISTANT', 'WORKER')")
    public ApiResponse<BlacklistPageResponseDTO> getHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.onSuccess(
                BlacklistSuccessCode.BLACKLISTS_FOUND,
                blacklistService.getHistory(userId, page, size)
        );
    }

    @Operation(summary = "블랙리스트 해제")
    @PatchMapping("/blacklists/{blacklistId}/release")
    @PreAuthorize("hasRole('ASSISTANT')")
    public ApiResponse<BlacklistResponseDTO> release(
            @AuthenticationPrincipal Long administratorId,
            @PathVariable Long blacklistId,
            @RequestBody @Valid ReleaseBlacklistRequestDTO request
    ) {
        return ApiResponse.onSuccess(
                BlacklistSuccessCode.BLACKLIST_RELEASED,
                blacklistService.release(administratorId, blacklistId, request.releaseReason())
        );
    }
}
