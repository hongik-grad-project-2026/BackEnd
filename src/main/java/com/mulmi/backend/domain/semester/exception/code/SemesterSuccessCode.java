package com.mulmi.backend.domain.semester.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SemesterSuccessCode implements BaseSuccessCode {

    // 명세 60번 (명세에 메시지가 없어 새로 정함)
    SEMESTER_SCHEDULE_FOUND(
            HttpStatus.OK,
            "SEMESTER200_1",
            "학기 일정을 조회했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
