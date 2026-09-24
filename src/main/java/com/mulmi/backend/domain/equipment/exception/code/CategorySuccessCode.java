package com.mulmi.backend.domain.equipment.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CategorySuccessCode implements BaseSuccessCode {
    NEW_CATEGORY(
            HttpStatus.CREATED,
            "CATEGORY201_1",
            "성공적으로 카테고리를 등록했습니다."),
    CATEGORIES_FOUND(
            HttpStatus.OK,
            "CATEGORY200_1",
            "기자재 카테고리를 조회했습니다."),
    UPDATED(
            HttpStatus.OK,
            "CATEGORY200_2",
            "카테고리가 수정되었습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
