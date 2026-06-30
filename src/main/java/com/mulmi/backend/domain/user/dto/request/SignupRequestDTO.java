package com.mulmi.backend.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignupRequestDTO(

        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @NotBlank(message = "학번은 필수입니다.")
        @Pattern(regexp = "^[A-Z][0-9]{6}$", message = "학번은 대문자1개와 숫자 6개 형식이어야 합니다.")
        String studentId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "전화번호는 필수입니다.")
        String phoneNumber,

        @NotBlank(message = "단과대학은 필수입니다.")
        String college,

        @NotBlank(message = "학과(전공)은 필수입니다.")
        String dept
) {
}