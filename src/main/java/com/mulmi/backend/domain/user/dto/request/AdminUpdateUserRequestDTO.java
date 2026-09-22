package com.mulmi.backend.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUpdateUserRequestDTO(
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        String email,

        @Pattern(regexp = "^[0-9-]{9,20}$", message = "전화번호 형식이 올바르지 않습니다.")
        String phoneNumber,

        @Size(max = 100, message = "단과대학은 100자 이하여야 합니다.")
        String college,

        @Size(max = 100, message = "학과는 100자 이하여야 합니다.")
        String department
) {
}
