package com.mulmi.backend.domain.notice.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NoticeSuccessCode implements BaseSuccessCode {
    NOTICES_FOUND(
            HttpStatus.OK,
            "NOTICE200_1",
            "공지사항 목록을 조회했습니다."
    ),
    NOTICE_FOUND(
            HttpStatus.OK,
            "NOTICE200_2",
            "공지사항을 조회했습니다."
    ),
    NOTICE_CREATED(
            HttpStatus.CREATED,
            "NOTICE201_1",
            "공지사항을 작성했습니다."
    ),
    NOTICE_UPDATED(
            HttpStatus.OK,
            "NOTICE200_3",
            "공지사항을 수정했습니다."
    ),
    NOTICE_DELETED(
            HttpStatus.OK,
            "NOTICE200_4",
            "공지사항을 삭제했습니다."
    ),
    ATTACHMENT_UPLOADED(
            HttpStatus.CREATED,
            "NOTICE201_2",
            "공지사항 첨부파일을 업로드했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
