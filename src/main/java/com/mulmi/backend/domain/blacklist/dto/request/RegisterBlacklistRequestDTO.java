package com.mulmi.backend.domain.blacklist.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterBlacklistRequestDTO(
        @NotNull Long semesterId,
        @NotBlank @Size(max = 500) String reasonDetail
) {
}
