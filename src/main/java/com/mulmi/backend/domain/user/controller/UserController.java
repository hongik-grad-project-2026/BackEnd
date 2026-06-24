package com.mulmi.backend.domain.user.controller;

import com.mulmi.backend.domain.user.dto.request.SignupRequestDTO;
import com.mulmi.backend.domain.user.dto.response.SignupResponseDTO;
import com.mulmi.backend.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    //회원가입
    public ApiResponse<SignupResponseDTO> signup(
            @RequestBody SignupRequestDTO dto){
        return null;
    }

    //로그인

}
