package com.mulmi.backend.domain.blacklist.exception;

import com.mulmi.backend.global.apiPayload.code.BaseErrorCode;
import com.mulmi.backend.global.apiPayload.exception.GeneralException;

public class BlacklistException extends GeneralException {
    public BlacklistException(BaseErrorCode code) {
        super(code);
    }
}
