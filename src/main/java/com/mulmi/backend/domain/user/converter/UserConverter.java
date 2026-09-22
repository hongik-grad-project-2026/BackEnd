package com.mulmi.backend.domain.user.converter;

import com.mulmi.backend.domain.user.dto.request.SignupRequestDTO;
import com.mulmi.backend.domain.user.dto.response.MyInfoResponseDTO;
import com.mulmi.backend.domain.user.dto.response.AdminUserSummaryResponseDTO;
import com.mulmi.backend.domain.user.dto.response.AdminUserDetailResponseDTO;
import com.mulmi.backend.domain.user.dto.response.SignupResponseDTO;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.domain.user.enums.UserRole;
import com.mulmi.backend.domain.user.enums.UserStatus;

public class UserConverter {

    //dto -> User로 변환
    public static User toUser(
            SignupRequestDTO request, String encodedPassword){
        return User.builder()
                .loginId(request.studentId())
                .studentId(request.studentId())
                .password(encodedPassword)
                .name(request.name())
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .college(request.college())
                .department(request.department())
                .role(UserRole.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();
    }

    //User->dto로 변환
    public static SignupResponseDTO toSignupResponseDTO(User user){
        return new SignupResponseDTO(
                user.getId(),
                user.getLoginId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getCollege(),
                user.getDepartment(),
                user.getRole(),
                user.getStatus()
        );
    }

    public static MyInfoResponseDTO toMyInfoResponseDTO(User user) {
        return new MyInfoResponseDTO(
                user.getId(),
                user.getLoginId(),
                user.getName(),
                user.getStudentId(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getCollege(),
                user.getDepartment(),
                user.getProfileImageUrl(),
                user.getRole(),
                user.getStatus()
        );
    }

    public static AdminUserSummaryResponseDTO toAdminUserSummaryResponseDTO(User user) {
        return new AdminUserSummaryResponseDTO(
                user.getId(),
                user.getName(),
                user.getStudentId(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getCollege(),
                user.getDepartment(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }

    public static AdminUserDetailResponseDTO toAdminUserDetailResponseDTO(User user) {
        return new AdminUserDetailResponseDTO(
                user.getId(),
                user.getLoginId(),
                user.getName(),
                user.getStudentId(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getCollege(),
                user.getDepartment(),
                user.getProfileImageUrl(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

}
