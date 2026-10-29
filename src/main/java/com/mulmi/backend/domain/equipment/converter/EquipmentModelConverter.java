package com.mulmi.backend.domain.equipment.converter;

import com.mulmi.backend.domain.equipment.dto.request.EquipmentModelCreateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.response.AdminEquipmentModelSummaryResponseDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentModelCreateResponseDTO;
import com.mulmi.backend.domain.equipment.entity.Category;
import com.mulmi.backend.domain.equipment.entity.EquipmentItem;
import com.mulmi.backend.domain.equipment.entity.EquipmentModel;
import com.mulmi.backend.domain.equipment.entity.LabelPrefix;
import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;

public class EquipmentModelConverter {

    // 요청 DTO + 조회해 온 카테고리,접두사 -> EquipmentModel
    public static EquipmentModel toEquipmentModel(
            EquipmentModelCreateRequestDTO dto, Category category, LabelPrefix labelPrefix) {
        return EquipmentModel.builder()
                .category(category)
                .labelPrefix(labelPrefix)
                .name(dto.modelName().trim())
                .components(dto.components())
                .specification(dto.specification())
                .description(dto.description())
                .rentalStatus(dto.rentalStatus())
                .disposalDate(dto.disposalDate())
                .build();
    }

    // 모델 + 발급 번호 -> EquipmentItem 한 대
    public static EquipmentItem toEquipmentItem(EquipmentModel model, int labelNumber) {
        String labelCode = String.format("%s-%03d", model.getLabelPrefix().getCode(), labelNumber);
        return EquipmentItem.builder()
                .equipmentModel(model)
                .labelNumber(labelNumber)
                .labelCode(labelCode)
                .status(EquipmentItemStatus.AVAILABLE)
                .build();
    }

    // 저장된 모델 + 생성된 아이템 목록 -> 응답 DTO
    public static EquipmentModelCreateResponseDTO toEquipmentModelCreateResponseDTO(
            EquipmentModel model) {
        return new EquipmentModelCreateResponseDTO(
                model.getId()
        );
    }

    // 모델 + 이미지 URL + 재고 -> 관리자 목록 한 줄
    public static AdminEquipmentModelSummaryResponseDTO toAdminEquipmentModelSummaryResponseDTO(
            EquipmentModel model, String modelImageUrl, long availableCount, long totalCount) {
        return new AdminEquipmentModelSummaryResponseDTO(
                model.getId(),
                modelImageUrl,
                model.getName(),
                model.getCategory().getId(),
                model.getCategory().getName(),
                model.getComponents(),
                model.getSpecification(),
                model.getDescription(),
                availableCount,
                totalCount,
                model.getRentalStatus(),
                model.getCreatedAt().toLocalDate(),
                model.getDisposalDate()
        );
    }
}
