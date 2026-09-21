package com.mulmi.backend.domain.user.controller;

import com.mulmi.backend.domain.user.exception.code.UserSuccessCode;
import com.mulmi.backend.domain.user.service.UserService;
import com.mulmi.backend.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;

    // 로그아웃
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        userService.logout(authorizationHeader.substring(7));
        return ApiResponse.onSuccess(UserSuccessCode.LOGOUT_SUCCESS, null);
    }
}
