package com.mulmi.backend.domain.user.dto.response;

import com.mulmi.backend.domain.user.enums.UserRole;

public record LoginResponseDTO(
        Long userId,
        String loginId,
        String name,
        UserRole role,
        String accessToken
) {
}
