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
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
