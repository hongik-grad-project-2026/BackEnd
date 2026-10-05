package com.mulmi.backend.domain.equipment.converter;

import com.mulmi.backend.domain.equipment.dto.request.EquipmentModelCreateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentModelCreateResponseDTO;
import com.mulmi.backend.domain.equipment.entity.Category;
import com.mulmi.backend.domain.equipment.entity.EquipmentItem;
import com.mulmi.backend.domain.equipment.entity.EquipmentModel;
import com.mulmi.backend.domain.equipment.entity.LabelPrefix;
import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;
import com.mulmi.backend.domain.equipment.enums.EquipmentModelStatus;

import java.util.List;

public class EquipmentModelConverter {

    // 요청 DTO + 조회해 온 카테고리,접두사 -> EquipmentModel
    public static EquipmentModel toEquipmentModel(
            EquipmentModelCreateRequestDTO dto, Category category, LabelPrefix labelPrefix) {
        return EquipmentModel.builder()
                .category(category)
                .labelPrefix(labelPrefix)
                .name(dto.modelName())
                .components(dto.components())
                .specification(dto.specification())
                .description(dto.description())
                .applicationPolicy(dto.applicationPolicy())
                .loanPeriodType(dto.loanPeriodType())
                .loanDurationDays(dto.loanDurationDays())
                .pledgeRequired(dto.pledgeRequired())
                .rentalStatus(EquipmentModelStatus.AVAILABLE)
                .build();
    }

    // 모델 + 비품번호 + 발급 번호 -> EquipmentItem 한 대
    public static EquipmentItem toEquipmentItem(EquipmentModel model, String assetNo, int labelNumber) {
        String labelCode = String.format("%s-%03d", model.getLabelPrefix().getCode(), labelNumber);
        return EquipmentItem.builder()
                .equipmentModel(model)
                .assetNo(assetNo)
                .labelNumber(labelNumber)
                .labelCode(labelCode)
                .status(EquipmentItemStatus.AVAILABLE)
                .build();
    }

    // 저장된 모델 + 생성된 아이템 목록 -> 응답 DTO
    public static EquipmentModelCreateResponseDTO toEquipmentModelCreateResponseDTO(
            EquipmentModel model, List<EquipmentItem> items) {
        return new EquipmentModelCreateResponseDTO(
                model.getId(),
                items.size(),
                items.get(0).getLabelCode(),
                items.get(items.size() - 1).getLabelCode()
        );
    }
}
