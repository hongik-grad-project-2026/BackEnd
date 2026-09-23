package com.mulmi.backend.domain.user.controller;

import com.mulmi.backend.domain.user.dto.request.LoginRequestDTO;
import com.mulmi.backend.domain.user.dto.response.LoginResponseDTO;
import com.mulmi.backend.domain.user.exception.code.UserSuccessCode;
import com.mulmi.backend.domain.user.service.UserService;
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
@RequestMapping("/api")
@Tag(name = "인증", description = "로그인 및 로그아웃 API")
public class AuthController {

    private final UserService userService;

    // 로그인
    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ApiResponse<LoginResponseDTO> login(
            @RequestBody @Valid LoginRequestDTO dto) {
        return ApiResponse.onSuccess(
                UserSuccessCode.LOGIN_SUCCESS,
                userService.login(dto)
        );
    }

    // 로그아웃
    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.onSuccess(UserSuccessCode.LOGOUT_SUCCESS, null);
    }
}
