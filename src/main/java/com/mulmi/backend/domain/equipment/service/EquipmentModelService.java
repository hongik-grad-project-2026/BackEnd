package com.mulmi.backend.domain.equipment.service;

import com.mulmi.backend.domain.equipment.dto.request.EquipmentModelCreateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.response.AdminEquipmentModelPageResponseDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentModelCreateResponseDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentModelImageUploadResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface EquipmentModelService {

    // 모델 등록
    EquipmentModelCreateResponseDTO createEquipmentModel(EquipmentModelCreateRequestDTO dto);

    // 모델 이미지 등록
    EquipmentModelImageUploadResponseDTO uploadModelImage(Long modelId, MultipartFile image);

    // 관리자 모델 목록 조회
    AdminEquipmentModelPageResponseDTO getAdminEquipmentModels(
            Long categoryId, String keyword, int page, int size);

}

