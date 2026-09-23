package com.mulmi.backend.domain.equipment.exception;

import com.mulmi.backend.global.apiPayload.code.BaseErrorCode;
import com.mulmi.backend.global.apiPayload.exception.GeneralException;

public class CategoryException extends GeneralException {
    public CategoryException(BaseErrorCode code) {
        super(code);
    }
}
