package com.mulmi.backend.domain.equipment.dto.request;

import com.mulmi.backend.domain.equipment.enums.EquipmentModelStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EquipmentModelCreateRequestDTO(
        @NotNull(message = "카테고리는 필수입니다.")
        Long categoryId,

        @Size(max = 10)
        String prefixCode,

        @Size(max = 100, message = "모델명은 100자를 넘을 수 없습니다.")
        @NotBlank(message = "모델명은 필수입니다.")
        String modelName,

        @NotBlank(message = "구성품은 필수입니다.")
        String components,

        String description,
        String specification,

        @NotNull(message = "상태는 필수입니다.")
        EquipmentModelStatus rentalStatus,

        @NotNull(message = "폐기일은 필수입니다.")
        LocalDate disposalDate
        ) {
}
