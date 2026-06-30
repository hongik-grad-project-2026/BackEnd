package com.mulmi.backend.domain.user.service;

import com.mulmi.backend.domain.user.dto.request.SignupRequestDTO;
import com.mulmi.backend.domain.user.dto.response.SignupResponseDTO;

public interface UserService {

    //회원가입
    SignupResponseDTO signup(
            SignupRequestDTO dto
    );
}
