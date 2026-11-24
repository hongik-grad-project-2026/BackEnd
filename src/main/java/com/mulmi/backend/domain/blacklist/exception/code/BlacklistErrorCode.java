package com.mulmi.backend.domain.blacklist.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BlacklistErrorCode implements BaseErrorCode {

    BLACKLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "BLACKLIST404_1", "블랙리스트 기록을 찾을 수 없습니다."),
    SEMESTER_NOT_FOUND(HttpStatus.NOT_FOUND, "BLACKLIST404_2", "학기 정보를 찾을 수 없습니다."),
    ACTIVE_BLACKLIST_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "BLACKLIST409_1",
            "해당 학기에 이미 적용 중인 대여 제한이 있습니다."
    ),
    BLACKLIST_ALREADY_RELEASED(HttpStatus.CONFLICT, "BLACKLIST409_2", "이미 해제된 블랙리스트입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
