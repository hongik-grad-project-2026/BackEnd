package com.mulmi.backend.domain.user.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    DUPLICATE_STUDENT_ID(
            HttpStatus.CONFLICT,
            "USER409_1",
            "이미 가입된 학번입니다."
    ),

    DUPLICATE_EMAIL(
            HttpStatus.CONFLICT,
            "USER409_2",
            "이미 사용 중인 이메일입니다."
    ),

    DUPLICATE_LOGIN_ID(
            HttpStatus.CONFLICT,
            "USER409_3",
            "이미 사용 중인 아이디입니다."
    ),

    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "USER404_1",
            "사용자를 찾을 수 없습니다."
    ),

    INVALID_CREDENTIALS(
            HttpStatus.UNAUTHORIZED,
            "USER401_1",
            "아이디 또는 비밀번호가 일치하지 않습니다."
    ),

    INACTIVE_USER(
            HttpStatus.FORBIDDEN,
            "USER403_1",
            "사용할 수 없는 계정입니다."
    ),

    EMPTY_UPDATE_REQUEST(
            HttpStatus.BAD_REQUEST,
            "USER400_1",
            "수정할 정보를 입력해 주세요."
    ),

    INVALID_PROFILE_IMAGE(
            HttpStatus.BAD_REQUEST,
            "USER400_2",
            "JPG, PNG, WEBP 형식의 프로필 이미지만 업로드할 수 있습니다."
    ),

    PROFILE_IMAGE_TOO_LARGE(
            HttpStatus.PAYLOAD_TOO_LARGE,
            "USER413_1",
            "프로필 이미지는 5MB를 넘을 수 없습니다."
    ),

    PASSWORD_RESET_VERIFICATION_NOT_FOUND(
            HttpStatus.BAD_REQUEST,
            "USER400_3",
            "인증정보가 일치하지 않습니다."
    ),

    PASSWORD_RESET_CODE_INVALID(
            HttpStatus.BAD_REQUEST,
            "USER400_4",
            "인증번호가 올바르지 않습니다."
    ),

    PASSWORD_RESET_CODE_EXPIRED(
            HttpStatus.GONE,
            "USER410_1",
            "인증번호가 만료되었습니다."
    ),

    PASSWORD_RESET_TOO_MANY_ATTEMPTS(
            HttpStatus.TOO_MANY_REQUESTS,
            "USER429_1",
            "인증번호 입력 가능 횟수를 초과했습니다. 인증번호를 다시 요청해 주세요."
    ),

    PASSWORD_RESET_RESEND_TOO_SOON(
            HttpStatus.TOO_MANY_REQUESTS,
            "USER429_2",
            "인증번호는 1분 후 다시 요청할 수 있습니다."
    ),

    PASSWORD_RESET_TOKEN_INVALID(
            HttpStatus.BAD_REQUEST,
            "USER400_5",
            "비밀번호 재설정 요청이 올바르지 않습니다."
    ),

    PASSWORD_RESET_TOKEN_EXPIRED(
            HttpStatus.GONE,
            "USER410_2",
            "비밀번호 재설정 시간이 만료되었습니다."
    ),

    PASSWORD_CONFIRM_MISMATCH(
            HttpStatus.BAD_REQUEST,
            "USER400_6",
            "새 비밀번호와 비밀번호 확인이 일치하지 않습니다."
    ),

    PASSWORD_RESET_MAIL_FAILED(
            HttpStatus.SERVICE_UNAVAILABLE,
            "USER503_1",
            "인증 메일을 전송할 수 없습니다. 잠시 후 다시 시도해 주세요."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
