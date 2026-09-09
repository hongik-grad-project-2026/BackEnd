package com.mulmi.backend.domain.user.dto.response;

import com.mulmi.backend.domain.user.enums.UserRole;
import com.mulmi.backend.domain.user.enums.UserStatus;

public record MyInfoResponseDTO(
        Long userId,
        String loginId,
        String name,
        String studentId,
        String email,
        String phoneNumber,
        String college,
        String department,
        String profileImageUrl,
        UserRole role,
        UserStatus status
) {
}
