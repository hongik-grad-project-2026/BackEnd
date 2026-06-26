package com.mulmi.backend.domain.user.exception;

import com.mulmi.backend.global.apiPayload.code.BaseErrorCode;
import com.mulmi.backend.global.apiPayload.exception.GeneralException;

public class UserException extends GeneralException {
    public UserException(BaseErrorCode code){
        super(code);
    }
}
