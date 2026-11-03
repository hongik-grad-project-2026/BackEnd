package com.mulmi.backend.domain.equipment.dto.request;

import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EquipmentItemCreateRequestDTO(
        @NotNull(message = "모델은 필수입니다.")
        Long modelId,

        @NotNull(message = "수량은 필수입니다.")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
        Integer quantity,

        // 라벨 쓰는 카테고리면 필수, 기타 카테고리면 null (서비스에서 검사)
        @Min(value = 1, message = "라벨 시작 번호는 1 이상이어야 합니다.")
        Integer labelStart,

        @NotNull(message = "상태는 필수입니다.")
        EquipmentItemStatus status,

        String memo
) {
}
