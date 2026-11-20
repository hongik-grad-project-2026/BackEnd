package com.mulmi.backend.domain.equipment.controller;

import com.mulmi.backend.domain.equipment.dto.response.DashboardCategoryUsageResponseDTO;
import com.mulmi.backend.domain.equipment.dto.response.DashboardIdleModelResponseDTO;
import com.mulmi.backend.domain.equipment.exception.code.DashboardSuccessCode;
import com.mulmi.backend.domain.equipment.service.DashboardService;
import com.mulmi.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
@Tag(name = "대시보드", description = "조교용 대시보드 API")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    // 62. 모델 가동률 TOP3
    @Operation(summary = "모델 가동률 TOP3", description = "노트북·태블릿 카테고리별 가동률 상위 3개 모델")
    @GetMapping("/model-utilization")
    @PreAuthorize("hasRole('ASSISTANT')")
    public ApiResponse<List<DashboardCategoryUsageResponseDTO>> getModelUsage() {
        return ApiResponse.onSuccess(
                DashboardSuccessCode.MODEL_USAGE_FOUND,
                dashboardService.getModelUsage()
        );
    }

    // 63. 장기 미대여 모델
    @Operation(summary = "장기 미대여 모델", description = "마지막 반납(없으면 등록) 후 14일 이상 대여되지 않은 모델, 미대여 일수 상위 3개")
    @GetMapping("/long-term-idle")
    @PreAuthorize("hasRole('ASSISTANT')")
    public ApiResponse<List<DashboardIdleModelResponseDTO>> getLongTermIdleModels() {
        return ApiResponse.onSuccess(
                DashboardSuccessCode.LONG_TERM_IDLE_FOUND,
                dashboardService.getLongTermIdleModels()
        );
    }

}
