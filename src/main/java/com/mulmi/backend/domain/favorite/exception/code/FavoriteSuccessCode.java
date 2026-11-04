package com.mulmi.backend.domain.favorite.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FavoriteSuccessCode implements BaseSuccessCode {

    FAVORITE_ADDED(
            HttpStatus.CREATED,
            "FAVORITE201_1",
            "찜 목록에 추가했습니다."
    ),

    FAVORITE_REMOVED(
            HttpStatus.OK,
            "FAVORITE200_1",
            "찜 목록에서 삭제했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
