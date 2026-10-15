package com.mulmi.backend.domain.user.controller;

import com.mulmi.backend.domain.user.dto.request.PasswordResetCodeConfirmRequestDTO;
import com.mulmi.backend.domain.user.dto.request.PasswordResetCodeRequestDTO;
import com.mulmi.backend.domain.user.dto.request.PasswordResetRequestDTO;
import com.mulmi.backend.domain.user.dto.response.PasswordResetResponseDTO;
import com.mulmi.backend.domain.user.dto.response.PasswordResetTokenResponseDTO;
import com.mulmi.backend.domain.user.exception.code.UserSuccessCode;
import com.mulmi.backend.domain.user.service.PasswordResetService;
import com.mulmi.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/password-reset")
@Tag(name = "비밀번호 재설정", description = "비밀번호 찾기 및 재설정 API")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    // 비밀번호 재설정 인증번호 발송
    @Operation(summary = "비밀번호 재설정 인증번호 발송")
    @PostMapping("/verifications")
    public ApiResponse<Void> sendVerificationCode(
            @RequestBody @Valid PasswordResetCodeRequestDTO dto
    ) {
        passwordResetService.sendVerificationCode(dto);
        return ApiResponse.onSuccess(UserSuccessCode.PASSWORD_RESET_CODE_SENT, null);
    }

    // 비밀번호 재설정 인증번호 확인
    @Operation(summary = "비밀번호 재설정 인증번호 확인")
    @PostMapping("/verifications/confirm")
    public ApiResponse<PasswordResetTokenResponseDTO> confirmVerificationCode(
            @RequestBody @Valid PasswordResetCodeConfirmRequestDTO dto
    ) {
        return ApiResponse.onSuccess(
                UserSuccessCode.PASSWORD_RESET_VERIFIED,
                passwordResetService.confirmVerificationCode(dto)
        );
    }

    // 새 비밀번호로 재설정
    @Operation(summary = "비밀번호 재설정")
    @PostMapping
    public ApiResponse<PasswordResetResponseDTO> resetPassword(
            @RequestBody @Valid PasswordResetRequestDTO dto
    ) {
        return ApiResponse.onSuccess(
                UserSuccessCode.PASSWORD_RESET_COMPLETED,
                passwordResetService.resetPassword(dto)
        );
    }
}
