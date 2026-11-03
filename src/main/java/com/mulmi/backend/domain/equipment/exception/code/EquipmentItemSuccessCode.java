package com.mulmi.backend.domain.equipment.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EquipmentItemSuccessCode implements BaseSuccessCode {

    ITEMS_CREATED(
            HttpStatus.CREATED,
            "EQUIPMENTITEM201_1",
            "기자재를 일괄 등록했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
