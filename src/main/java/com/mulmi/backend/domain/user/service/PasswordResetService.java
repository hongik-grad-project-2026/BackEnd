package com.mulmi.backend.domain.user.service;

import com.mulmi.backend.domain.user.dto.request.PasswordResetCodeConfirmRequestDTO;
import com.mulmi.backend.domain.user.dto.request.PasswordResetCodeRequestDTO;
import com.mulmi.backend.domain.user.dto.request.PasswordResetRequestDTO;
import com.mulmi.backend.domain.user.dto.response.PasswordResetResponseDTO;
import com.mulmi.backend.domain.user.dto.response.PasswordResetTokenResponseDTO;

public interface PasswordResetService {

    void sendVerificationCode(PasswordResetCodeRequestDTO dto);

    PasswordResetTokenResponseDTO confirmVerificationCode(
            PasswordResetCodeConfirmRequestDTO dto
    );

    PasswordResetResponseDTO resetPassword(PasswordResetRequestDTO dto);
}
