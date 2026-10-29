package com.mulmi.backend.domain.equipment.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EquipmentModelSuccessCode implements BaseSuccessCode {
    IMAGE_UPLOADED(
            HttpStatus.OK,
            "EQUIPMENTMODEL200_1",
            "기자재 모델 이미지를 등록했습니다."
    ),

    MODELS_FOUND(
            HttpStatus.OK,
            "EQUIPMENTMODEL200_2",
            "기자재 모델 목록을 조회했습니다."
    ),

    MODEL_FOUND(
            HttpStatus.OK,
            "EQUIPMENTMODEL200_3",
            "기자재 모델을 조회했습니다."
    ),

    MODEL_CREATED(
            HttpStatus.CREATED,
            "EQUIPMENTMODEL201_1",
            "기자재 모델을 등록했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;

}
