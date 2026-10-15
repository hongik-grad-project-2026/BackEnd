package com.mulmi.backend.domain.user.dto.response;

public record PasswordResetResponseDTO(
        String studentId,
        String name,
        String email,
        String college,
        String department
) {
}
