package com.mulmi.backend.domain.equipment.service;

import com.mulmi.backend.domain.equipment.dto.request.EquipmentItemCreateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentItemCreateResponseDTO;

public interface EquipmentItemService {

    // 기기 일괄 등록
    EquipmentItemCreateResponseDTO createEquipmentItems(EquipmentItemCreateRequestDTO dto);
}
