package com.mulmi.backend.global.apiPayload.exception;

import com.mulmi.backend.global.apiPayload.ApiResponse;
import com.mulmi.backend.global.apiPayload.code.GeneralErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 예외 처리
     */
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(GeneralException e) {
        log.warn("[GeneralException] Code: {}, Message: {}",
                e.getCode().getCode(),
                e.getCode().getMessage()
        );

        return ResponseEntity
                .status(e.getCode().getStatus())
                .body(ApiResponse.onFailure(e.getCode(), null));
    }

    /**
     * @Valid 유효성 검사 실패 처리 RequestBody
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        log.warn("[MethodArgumentNotValid] Url: {}, Message: {}",
                request.getRequestURI(),
                e.getMessage()
        );

        Map<String, String> errors = new LinkedHashMap<>();

        e.getBindingResult().getFieldErrors().forEach(fieldError -> {
            String fieldName = fieldError.getField();
            String errorMessage = Optional.ofNullable(fieldError.getDefaultMessage()).orElse("");
            errors.merge(fieldName, errorMessage, (existing, newMsg) -> existing + ", " + newMsg);
        });

        return ResponseEntity
                .status(GeneralErrorCode.BAD_REQUEST.getStatus())
                .body(ApiResponse.onFailure(
                        GeneralErrorCode.BAD_REQUEST,
                        errors
                ));
    }

    /**
     * @Validated 유효성 검사 실패 처리 RequestParam, PathVariable
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(
            ConstraintViolationException e,
            HttpServletRequest request
    ) {
        log.warn("[ConstraintViolation] Url: {}, Message: {}",
                request.getRequestURI(),
                e.getMessage()
        );

        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("Invalid input");

        return ResponseEntity
                .status(GeneralErrorCode.BAD_REQUEST.getStatus())
                .body(ApiResponse.onFailure(
                        GeneralErrorCode.BAD_REQUEST,
                        errorMessage
                ));
    }

    /**
     * 기타 서버 내부 에러 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(
            Exception e,
            HttpServletRequest request
    ) {
        log.error("[Exception] Url: {}, Message: {}",
                request.getRequestURI(),
                e.getMessage(),
                e
        );

        return ResponseEntity
                .status(GeneralErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.onFailure(
                        GeneralErrorCode.INTERNAL_SERVER_ERROR,
                        e.getMessage()
                ));
    }
}