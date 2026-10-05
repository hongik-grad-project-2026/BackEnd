package com.mulmi.backend.domain.equipment.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EquipmentModelErrorCode implements BaseErrorCode {
    INVALID_EQUIPMENT_POLICY(
            HttpStatus.BAD_REQUEST,
            "EQUIPMENTMODEL400_1",
            "대여 정책 조합이 올바르지 않습니다."
    ),

    INVALID_INITIAL_QUANTITY(
            HttpStatus.BAD_REQUEST,
            "EQUIPMENTMODEL400_2",
            "비품번호 목록의 개수가 등록 수량과 다릅니다."
    ),

    DUPLICATE_MODEL(
            HttpStatus.CONFLICT,
            "EQUIPMENTMODEL409_1",
            "같은 카테고리에 동일 이름의 모델이 이미 있습니다."
    ),

    DUPLICATE_ASSET_NO(
            HttpStatus.CONFLICT,
            "EQUIPMENTMODEL409_2",
            "해당 비품번호가 이미 있습니다."
    );



    private final HttpStatus status;
    private final String code;
    private final String message;
}
