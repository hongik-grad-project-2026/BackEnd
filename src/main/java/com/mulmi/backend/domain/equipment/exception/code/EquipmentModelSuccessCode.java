package com.mulmi.backend.domain.equipment.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EquipmentModelSuccessCode implements BaseSuccessCode {
    MODEL_CREATED(
            HttpStatus.CREATED,
            "EQUIPMENTMODEL201_1",
            "기자재 모델과 기자재를 등록했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;

}
