package com.mulmi.backend.domain.equipment.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LabelPrefixErrorCode implements BaseErrorCode {
    LABEL_PREFIX_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "LABELPREFIX404_1",
            "라벨 접두사를 찾을 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
