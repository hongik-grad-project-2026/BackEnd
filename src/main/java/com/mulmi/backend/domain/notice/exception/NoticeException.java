package com.mulmi.backend.domain.notice.exception;

import com.mulmi.backend.global.apiPayload.code.BaseErrorCode;
import com.mulmi.backend.global.apiPayload.exception.GeneralException;

public class NoticeException extends GeneralException {

    public NoticeException(BaseErrorCode code) {
        super(code);
    }
}
