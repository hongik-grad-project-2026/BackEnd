package com.mulmi.backend.domain.equipment.service;

import com.mulmi.backend.domain.equipment.dto.request.EquipmentModelCreateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.request.EquipmentModelUpdateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentModelUpdateResponseDTO;
import com.mulmi.backend.domain.equipment.dto.response.AdminEquipmentItemPageResponseDTO;
import com.mulmi.backend.domain.equipment.dto.response.AdminEquipmentModelDetailResponseDTO;
import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;
import com.mulmi.backend.domain.equipment.dto.response.AdminEquipmentModelPageResponseDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentModelCreateResponseDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentModelImageUploadResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface EquipmentModelService {

    // 모델 등록
    EquipmentModelCreateResponseDTO createEquipmentModel(EquipmentModelCreateRequestDTO dto);

    // 모델 수정
    EquipmentModelUpdateResponseDTO updateEquipmentModel(Long modelId, EquipmentModelUpdateRequestDTO dto);

    // 모델 폐기 (소프트 삭제, 남은 기기도 함께 폐기)
    void retireEquipmentModel(Long modelId);

    // 모델 이미지 등록
    EquipmentModelImageUploadResponseDTO uploadModelImage(Long modelId, MultipartFile image);

    // 관리자 모델 목록 조회
    AdminEquipmentModelPageResponseDTO getAdminEquipmentModels(
            Long userId, Long categoryId, String keyword, int page, int size);

    // 관리자 모델 단건 조회 (기기 목록 페이지 머리말)
    AdminEquipmentModelDetailResponseDTO getAdminEquipmentModel(Long modelId);

    // 모델별 기기 목록 조회 (상태별 개수 포함)
    AdminEquipmentItemPageResponseDTO getAdminEquipmentItems(
            Long modelId, EquipmentItemStatus status, String keyword, int page, int size);

}

