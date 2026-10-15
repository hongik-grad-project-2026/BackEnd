package com.mulmi.backend.domain.user.dto.response;

public record PasswordResetTokenResponseDTO(
        String resetToken,
        long expiresInSeconds
) {
}
