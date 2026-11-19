package com.mulmi.backend.domain.blacklist.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReleaseBlacklistRequestDTO(
        @NotBlank @Size(max = 500) String releaseReason
) {
}
