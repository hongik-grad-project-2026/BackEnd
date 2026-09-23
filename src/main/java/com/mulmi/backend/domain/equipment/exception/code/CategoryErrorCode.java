package com.mulmi.backend.domain.equipment.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CategoryErrorCode implements BaseErrorCode {

    DUPLICATE_CATEGORY_NAME(
            HttpStatus.CONFLICT,
            "CATEGORY409_1",
            "이미 등록된 카테고리입니다."
    ),

    CATEGORY_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "CATEGORY404_1",
            "카테고리를 찾을 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;

}
