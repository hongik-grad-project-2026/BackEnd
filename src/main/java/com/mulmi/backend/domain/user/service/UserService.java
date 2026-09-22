package com.mulmi.backend.domain.user.service;

import com.mulmi.backend.domain.user.dto.request.LoginRequestDTO;
import com.mulmi.backend.domain.user.dto.request.SignupRequestDTO;
import com.mulmi.backend.domain.user.dto.request.UpdateMyInfoRequestDTO;
import com.mulmi.backend.domain.user.dto.response.LoginResponseDTO;
import com.mulmi.backend.domain.user.dto.response.MyInfoResponseDTO;
import com.mulmi.backend.domain.user.dto.response.SignupResponseDTO;
import com.mulmi.backend.domain.user.dto.response.AdminUserPageResponseDTO;
import com.mulmi.backend.domain.user.dto.response.AdminUserDetailResponseDTO;
import com.mulmi.backend.domain.user.dto.request.AdminUpdateUserRequestDTO;
import com.mulmi.backend.domain.user.enums.UserStatus;

public interface UserService {

    //회원가입
    SignupResponseDTO signup(
            SignupRequestDTO dto
    );

    // 로그인
    LoginResponseDTO login(
            LoginRequestDTO dto
    );

    MyInfoResponseDTO getMyInfo(Long userId);

    MyInfoResponseDTO updateMyInfo(Long userId, UpdateMyInfoRequestDTO dto);

    // 회원탈퇴
    void withdraw(Long userId);

    // 회원 목록 조회
    AdminUserPageResponseDTO getUsers(
            String keyword,
            UserStatus status,
            String college,
            String department,
            int page,
            int size
    );

    // 특정 학생 정보 조회
    AdminUserDetailResponseDTO getUser(Long userId);

    // 학생 정보 수정
    AdminUserDetailResponseDTO updateUser(Long userId, AdminUpdateUserRequestDTO dto);
}
