package com.mulmi.backend.domain.user.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserSuccessCode implements BaseSuccessCode {
    FOUND(HttpStatus.OK,
            "USER200_2",
            "성공적으로 사용자를 조회했습니다."),
    SIGNUP_SUCCESS(
            HttpStatus.CREATED,
            "USER201_1",
            "회원가입이 완료되었습니다."),
    LOGIN_SUCCESS(
            HttpStatus.OK,
            "USER200_1",
            "로그인이 완료되었습니다."
    ),
    UPDATED(
            HttpStatus.OK,
            "USER200_3",
            "사용자 정보가 수정되었습니다."
    ),
    WITHDRAWN(
            HttpStatus.OK,
            "USER200_4",
            "회원탈퇴가 완료되었습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;

}
