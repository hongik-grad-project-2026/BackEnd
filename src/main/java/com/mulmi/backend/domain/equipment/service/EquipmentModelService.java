package com.mulmi.backend.domain.equipment.service;

import com.mulmi.backend.domain.equipment.dto.request.EquipmentModelCreateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentModelCreateResponseDTO;

public interface EquipmentModelService {

    // 모델 등록
    EquipmentModelCreateResponseDTO createEquipmentModel(EquipmentModelCreateRequestDTO dto);

}

