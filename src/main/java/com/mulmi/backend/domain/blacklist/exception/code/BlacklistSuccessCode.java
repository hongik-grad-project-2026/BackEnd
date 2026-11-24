package com.mulmi.backend.domain.blacklist.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BlacklistSuccessCode implements BaseSuccessCode {

    BLACKLIST_REGISTERED(HttpStatus.CREATED, "BLACKLIST201_1", "블랙리스트를 등록했습니다."),
    BLACKLISTS_FOUND(HttpStatus.OK, "BLACKLIST200_1", "블랙리스트 이력을 조회했습니다."),
    BLACKLIST_RELEASED(HttpStatus.OK, "BLACKLIST200_2", "블랙리스트를 해제했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
