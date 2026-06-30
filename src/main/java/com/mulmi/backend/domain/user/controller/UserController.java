package com.mulmi.backend.domain.user.controller;

import com.mulmi.backend.domain.user.dto.request.SignupRequestDTO;
import com.mulmi.backend.domain.user.dto.response.SignupResponseDTO;
import com.mulmi.backend.domain.user.exception.code.UserSuccessCode;
import com.mulmi.backend.domain.user.service.UserService;
import com.mulmi.backend.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    //회원가입
    @PostMapping
    public ApiResponse<SignupResponseDTO> signup(
            @RequestBody @Valid SignupRequestDTO dto){
        return ApiResponse.onSuccess(UserSuccessCode.SIGNUP_SUCCESS, userService.signup(dto));
    }

    //로그인

}
