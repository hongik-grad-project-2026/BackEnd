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

    INVALID_ITEM_STATUS(
            HttpStatus.BAD_REQUEST,
            "EQUIPMENTITEM400_3",
            "대여 가능 또는 고장 상태만 지정할 수 있습니다."
    ),

    LABEL_NUMBER_NOT_ALLOWED(
            HttpStatus.BAD_REQUEST,
            "EQUIPMENTITEM400_4",
            "라벨을 쓰지 않는 카테고리의 기기에는 라벨 번호를 입력할 수 없습니다."
    ),

    ITEM_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "EQUIPMENTITEM404_1",
            "기기를 찾을 수 없습니다."
    ),

    DUPLICATE_LABEL(
            HttpStatus.CONFLICT,
            "EQUIPMENTITEM409_1",
            "이미 사용된 라벨입니다."
    ),

    ITEM_IN_USE(
            HttpStatus.CONFLICT,
            "EQUIPMENTITEM409_2",
            "대여 예정이거나 대여 중인 기기는 폐기할 수 없습니다."
    ),

    ITEM_ALREADY_DISPOSED(
            HttpStatus.CONFLICT,
            "EQUIPMENTITEM409_3",
            "이미 폐기된 기기입니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
