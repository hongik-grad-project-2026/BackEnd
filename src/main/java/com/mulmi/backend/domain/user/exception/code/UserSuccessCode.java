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
    LOGOUT_SUCCESS(
            HttpStatus.OK,
            "USER200_5",
            "로그아웃이 완료되었습니다."
    ),
    WITHDRAWN(
            HttpStatus.OK,
            "USER200_4",
            "회원탈퇴가 완료되었습니다."
    ),
    USERS_FOUND(
            HttpStatus.OK,
            "USER200_6",
            "회원 목록을 조회했습니다."
    ),
    USER_DETAIL_FOUND(
            HttpStatus.OK,
            "USER200_7",
            "학생 정보를 조회했습니다."
    ),
    USER_UPDATED_BY_ADMIN(
            HttpStatus.OK,
            "USER200_8",
            "학생 정보를 수정했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;

}
