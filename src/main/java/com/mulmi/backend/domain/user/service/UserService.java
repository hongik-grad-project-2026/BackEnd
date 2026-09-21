package com.mulmi.backend.domain.user.service;

import com.mulmi.backend.domain.user.dto.request.LoginRequestDTO;
import com.mulmi.backend.domain.user.dto.request.SignupRequestDTO;
import com.mulmi.backend.domain.user.dto.request.UpdateMyInfoRequestDTO;
import com.mulmi.backend.domain.user.dto.response.LoginResponseDTO;
import com.mulmi.backend.domain.user.dto.response.MyInfoResponseDTO;
import com.mulmi.backend.domain.user.dto.response.SignupResponseDTO;

public interface UserService {

    //회원가입
    SignupResponseDTO signup(
            SignupRequestDTO dto
    );

    //회원가입
    LoginResponseDTO login(
            LoginRequestDTO dto
    );

    // 로그아웃
    void logout(String accessToken);

    MyInfoResponseDTO getMyInfo(Long userId);

    MyInfoResponseDTO updateMyInfo(Long userId, UpdateMyInfoRequestDTO dto);
}
