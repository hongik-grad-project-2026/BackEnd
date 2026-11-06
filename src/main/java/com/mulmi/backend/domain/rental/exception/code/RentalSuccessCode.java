package com.mulmi.backend.domain.rental.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RentalSuccessCode implements BaseSuccessCode {

    // 메시지는 명세 46번 성공 응답
    RENTAL_CREATED(
            HttpStatus.CREATED,
            "RENTAL201_1",
            "대여 신청이 완료되었습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
