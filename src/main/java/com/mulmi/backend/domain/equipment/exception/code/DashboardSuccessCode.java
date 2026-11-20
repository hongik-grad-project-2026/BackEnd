package com.mulmi.backend.domain.equipment.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DashboardSuccessCode implements BaseSuccessCode {

    MODEL_USAGE_FOUND(
            HttpStatus.OK,
            "DASHBOARD200_1",
            "모델 가동률을 조회했습니다."
    ),

    LONG_TERM_IDLE_FOUND(
            HttpStatus.OK,
            "DASHBOARD200_2",
            "장기 미대여 모델을 조회했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
