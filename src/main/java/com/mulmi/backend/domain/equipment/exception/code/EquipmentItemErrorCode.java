package com.mulmi.backend.domain.equipment.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EquipmentItemErrorCode implements BaseErrorCode {

    LABEL_START_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "EQUIPMENTITEM400_1",
            "라벨을 쓰는 카테고리는 라벨 시작 번호가 필요합니다."
    ),

    LABEL_START_NOT_ALLOWED(
            HttpStatus.BAD_REQUEST,
            "EQUIPMENTITEM400_2",
            "라벨을 쓰지 않는 카테고리에는 라벨 시작 번호를 입력할 수 없습니다."
    ),

    DUPLICATE_LABEL(
            HttpStatus.CONFLICT,
            "EQUIPMENTITEM409_1",
            "등록하려는 라벨 범위에 이미 사용된 라벨이 있습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
