package com.mulmi.backend.domain.rental.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RentalErrorCode implements BaseErrorCode {

    INVALID_PICKUP_DATE(HttpStatus.BAD_REQUEST, "RENTAL400_1", "선택할 수 없는 수령희망일입니다."),

    BLACKLISTED_USER(HttpStatus.FORBIDDEN, "RENTAL403_1", "대여가 제한된 사용자입니다."),

    RENTAL_NOT_OPEN(HttpStatus.CONFLICT, "RENTAL409_1", "지금은 이 모델을 신청할 수 있는 기간이 아닙니다."),
    MODEL_NOT_RENTABLE(HttpStatus.CONFLICT, "RENTAL409_2", "대여가 중단된 모델입니다."),
    OUT_OF_STOCK(HttpStatus.CONFLICT, "RENTAL409_3", "대여 가능한 기기가 없습니다."),
    DUPLICATE_RENTAL(HttpStatus.CONFLICT, "RENTAL409_4", "같은 카테고리에서 진행 중인 대여가 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
