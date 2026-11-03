package com.mulmi.backend.domain.equipment.service;

import com.mulmi.backend.domain.equipment.dto.request.EquipmentItemCreateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.request.EquipmentItemUpdateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentItemCreateResponseDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentItemUpdateResponseDTO;

public interface EquipmentItemService {

    // 기기 일괄 등록
    EquipmentItemCreateResponseDTO createEquipmentItems(EquipmentItemCreateRequestDTO dto);

    // 기기 정보 수정
    EquipmentItemUpdateResponseDTO updateEquipmentItem(Long itemId, EquipmentItemUpdateRequestDTO dto);

    // 기기 폐기 (소프트 삭제)
    void disposeEquipmentItem(Long itemId);
}
