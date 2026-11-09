package com.mulmi.backend.domain.rental.exception;

import com.mulmi.backend.global.apiPayload.code.BaseErrorCode;
import com.mulmi.backend.global.apiPayload.exception.GeneralException;

public class RentalException extends GeneralException {
    public RentalException(BaseErrorCode code) {
        super(code);
    }
}
