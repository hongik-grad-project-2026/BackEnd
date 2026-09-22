package com.mulmi.backend.domain.user.dto.response;

import com.mulmi.backend.domain.user.enums.UserStatus;

import java.time.LocalDateTime;

public record AdminUserSummaryResponseDTO(
        Long userId,
        String name,
        String studentId,
        String email,
        String phoneNumber,
        String college,
        String department,
        UserStatus status,
        LocalDateTime createdAt
) {
}
