package com.mulmi.backend.domain.user.dto.response;

import com.mulmi.backend.domain.user.enums.UserRole;
import com.mulmi.backend.domain.user.enums.UserStatus;

public record SignupResponseDTO(
        Long userId,
        String loginId,
        String name,
        String email,
        String phoneNumber,
        String college,
        String dept,
        UserRole role,
        UserStatus status
) {
}