package com.mulmi.backend.domain.user.dto.response;

import com.mulmi.backend.domain.user.enums.UserStatus;

import java.time.LocalDateTime;

public record AdminUserDetailResponseDTO(
        Long userId,
        String loginId,
        String name,
        String studentId,
        String email,
        String phoneNumber,
        String college,
        String department,
        String profileImageUrl,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
