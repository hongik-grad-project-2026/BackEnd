package com.mulmi.backend.domain.notice.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NoticeErrorCode implements BaseErrorCode {
    NOTICE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "NOTICE404_1",
            "공지사항을 찾을 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
