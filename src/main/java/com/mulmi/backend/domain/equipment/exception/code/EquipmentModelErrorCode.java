package com.mulmi.backend.domain.equipment.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EquipmentModelErrorCode implements BaseErrorCode {

    LABEL_PREFIX_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "EQUIPMENTMODEL400_2",
            "라벨을 쓰는 카테고리는 접두사가 필요합니다."
    ),

    LABEL_PREFIX_NOT_ALLOWED(
            HttpStatus.BAD_REQUEST,
            "EQUIPMENTMODEL400_3",
            "라벨을 쓰지 않는 카테고리에는 접두사를 입력할 수 없습니다."
    ),

    INVALID_MODEL_IMAGE(
            HttpStatus.BAD_REQUEST,
            "EQUIPMENTMODEL400_4",
            "JPG, PNG, WEBP형식만 가능합니다."
    ),

    MODEL_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "EQUIPMENTMODEL404_1",
            "모델을 찾을 수 없습니다."
    ),

    DUPLICATE_MODEL(
            HttpStatus.CONFLICT,
            "EQUIPMENTMODEL409_1",
            "같은 카테고리에 동일 이름의 모델이 이미 있습니다."
    ),

    MODEL_IMAGE_TOO_LARGE(
            HttpStatus.CONTENT_TOO_LARGE,
            "EQUIPMENTMODEL413_1",
            "이미지 용량은 5MB이하만 가능합니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
