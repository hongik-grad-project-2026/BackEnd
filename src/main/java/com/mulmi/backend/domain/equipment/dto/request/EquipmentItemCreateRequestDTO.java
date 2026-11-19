package com.mulmi.backend.domain.equipment.dto.request;

import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EquipmentItemCreateRequestDTO(
        @NotNull(message = "모델은 필수입니다.")
        Long modelId,

        @NotNull(message = "수량은 필수입니다.")
        // 상한 200 (11-19 결정) : 없으면 실수로 큰 수를 넣었을 때 서버가 그만큼의 기기 목록을 만들다 메모리가 바닥난다
        @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
        @Max(value = 200, message = "수량은 200 이하여야 합니다.")
        Integer quantity,

        // 접두사 카테고리는 필수(없으면 400). 기타는 선택 — 없으면 서버가 기타 전체 최대 번호 + 1부터 발급 (11-19)
        @Min(value = 1, message = "라벨 시작 번호는 1 이상이어야 합니다.")
        Integer labelStart,

        @NotNull(message = "상태는 필수입니다.")
        EquipmentItemStatus status,

        String memo
) {
}
