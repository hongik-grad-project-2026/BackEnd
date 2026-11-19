package com.mulmi.backend.domain.equipment.dto.request;

import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EquipmentItemUpdateRequestDTO(
        // 모든 카테고리 입력 가능 (기타는 접두사 없이 007), null이면 기존 번호 유지
        @Min(value = 1, message = "라벨 번호는 1 이상이어야 합니다.")
        Integer labelNumber,

        @NotNull(message = "상태는 필수입니다.")
        EquipmentItemStatus status,

        // null이면 기존 메모 유지
        String memo
) {
}
