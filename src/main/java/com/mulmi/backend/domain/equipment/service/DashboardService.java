package com.mulmi.backend.domain.equipment.service;

import com.mulmi.backend.domain.equipment.dto.response.DashboardCategoryUsageResponseDTO;
import com.mulmi.backend.domain.equipment.dto.response.DashboardIdleModelResponseDTO;

import java.util.List;

public interface DashboardService {

    // 62. 모델 가동률 TOP3 (노트북·태블릿 카테고리별)
    List<DashboardCategoryUsageResponseDTO> getModelUsage();

    // 63. 장기 미대여 모델 (미대여 일수 상위 3개)
    List<DashboardIdleModelResponseDTO> getLongTermIdleModels();

}
