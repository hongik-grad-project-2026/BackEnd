package com.mulmi.backend.domain.user.dto.response;

import com.mulmi.backend.domain.user.enums.UserRole;
import com.mulmi.backend.domain.user.enums.UserStatus;

public record LoginResponseDTO(
        Long userId,
        String loginId,
        String name,
        UserRole role,
        UserStatus status,
        String acessToken) {}
