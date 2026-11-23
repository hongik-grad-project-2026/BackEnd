package com.mulmi.backend.domain.equipment.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EquipmentItemErrorCode implements BaseErrorCode {

    // 400_2(라벨 시작 번호 입력 불가)·400_4(라벨 번호 입력 불가)는 기타도 번호를 쓰게 되며 삭제 (11-19)
    //  번호를 당겨 매기지 않는다 : 이미 받은 코드 값의 뜻이 바뀌지 않게
    LABEL_START_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "EQUIPMENTITEM400_1",
            "라벨 시작 번호가 필요합니다."
    ),

    // 시작 번호 + 수량이 저장 가능한 번호(2147483647)를 넘음 (자동 발급 포함)
    LABEL_NUMBER_OUT_OF_RANGE(
            HttpStatus.BAD_REQUEST,
            "EQUIPMENTITEM400_5",
            "라벨 번호가 허용 범위를 넘습니다."
    ),

    INVALID_ITEM_STATUS(
            HttpStatus.BAD_REQUEST,
            "EQUIPMENTITEM400_3",
            "대여 가능 또는 고장 상태만 지정할 수 있습니다."
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
    ),

    // 11-07 결정 : 대여 흐름 중인 기기는 반려·반납으로 흐름을 끝낸 뒤에 수정한다
    ITEM_IN_RENTAL(
            HttpStatus.CONFLICT,
            "EQUIPMENTITEM409_5",
            "대여 예정이거나 대여 중인 기기는 수정할 수 없습니다. 반려 또는 반납 처리 후 수정해 주세요."
    );
    // 409_4(모델 정보 변경됨)는 기기 등록이 모델 행을 잠그게 되며 삭제 (11-19) — 잠금 동안 모델이 바뀔 수 없다

    private final HttpStatus status;
    private final String code;
    private final String message;
}
