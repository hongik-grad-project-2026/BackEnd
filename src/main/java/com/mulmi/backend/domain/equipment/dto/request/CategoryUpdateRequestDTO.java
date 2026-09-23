package com.mulmi.backend.domain.equipment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequestDTO(
        @NotBlank(message = "카테고리명은 필수입니다.")
        @Size(max = 50, message = "카테고리명은 50자를 넘을 수 없습니다.")
        String name
) {
}
