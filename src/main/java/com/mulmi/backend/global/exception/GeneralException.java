package com.mulmi.backend.global.exception;

import com.mulmi.backend.global.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException{
    private final BaseCode code;
}
