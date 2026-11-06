package com.mulmi.backend.domain.rental.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RentalErrorCode implements BaseErrorCode {

    INVALID_PICKUP_DATE(HttpStatus.BAD_REQUEST, "RENTAL400_1", "선택할 수 없는 수령희망일입니다."),
    INVALID_REJECT_REASON(HttpStatus.BAD_REQUEST, "RENTAL400_2", "기타 사유로 반려할 때는 상세 사유가 필요합니다."),
    INVALID_RENTAL_IMAGE(HttpStatus.BAD_REQUEST, "RENTAL400_3", "JPG, PNG, WEBP 형식의 이미지만 올릴 수 있습니다."),
    RENTAL_IMAGE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "RENTAL413_1", "이미지 용량은 10MB 이하만 가능합니다."),

    BLACKLISTED_USER(HttpStatus.FORBIDDEN, "RENTAL403_1", "대여가 제한된 사용자입니다."),
    // 신청(46)은 본인 계정, 승인(52)은 신청자 계정을 검사한다 (11-06 결정: 이유를 구분해서 알린다)
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "RENTAL403_2", "정지된 계정입니다."),
    ACCOUNT_WITHDRAWN(HttpStatus.FORBIDDEN, "RENTAL403_3", "탈퇴한 계정입니다."),

    RENTAL_NOT_FOUND(HttpStatus.NOT_FOUND, "RENTAL404_1", "대여 신청을 찾을 수 없습니다."),

    RENTAL_NOT_OPEN(HttpStatus.CONFLICT, "RENTAL409_1", "지금은 이 모델을 신청할 수 있는 기간이 아닙니다."),
    MODEL_NOT_RENTABLE(HttpStatus.CONFLICT, "RENTAL409_2", "대여가 중단된 모델입니다."),
    OUT_OF_STOCK(HttpStatus.CONFLICT, "RENTAL409_3", "대여 가능한 기기가 없습니다."),
    DUPLICATE_RENTAL(HttpStatus.CONFLICT, "RENTAL409_4", "같은 카테고리에서 진행 중인 대여가 있습니다."),
    INVALID_RENTAL_STATUS(HttpStatus.CONFLICT, "RENTAL409_5", "현재 상태에서는 처리할 수 없는 대여입니다."),
    PICKUP_DEADLINE_PASSED(HttpStatus.CONFLICT, "RENTAL409_6", "수령 기한이 지난 대여입니다."),
    PICKUP_IMAGE_REQUIRED(HttpStatus.CONFLICT, "RENTAL409_7", "수령 사진을 먼저 올려 주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
