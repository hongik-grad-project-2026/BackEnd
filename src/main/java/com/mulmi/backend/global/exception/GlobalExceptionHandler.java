package com.mulmi.backend.global.exception;

import com.mulmi.backend.global.apiPayload.ApiResponse;
import com.mulmi.backend.global.code.BaseErrorCode;
import com.mulmi.backend.global.code.ErrorReasonDTO;
import com.mulmi.backend.global.status.ErrorStatus;
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
        BaseErrorCode code = e.getCode();
        ErrorReasonDTO reason = code.getReasonHttpStatus();

        log.warn("[GeneralException] Code: {}, Message: {}", reason.getCode(), reason.getMessage());

        return ResponseEntity
                .status(reason.getHttpStatus())
                .body(ApiResponse.onFailure(
                        reason.getCode(),
                        reason.getMessage(),
                        null
                ));
    }

    /**
     * @Valid 유효성 검사 실패 처리 (RequestBody)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        log.warn("[MethodArgumentNotValid] Url: {}, Message: {}", request.getRequestURI(), e.getMessage());

        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fieldError -> {
            String fieldName = fieldError.getField();
            String errorMessage = Optional.ofNullable(fieldError.getDefaultMessage()).orElse("");
            errors.merge(fieldName, errorMessage, (existing, newMsg) -> existing + ", " + newMsg);
        });

        ErrorReasonDTO reason = ErrorStatus._BAD_REQUEST.getReasonHttpStatus();

        return ResponseEntity
                .status(reason.getHttpStatus())
                .body(ApiResponse.onFailure(
                        reason.getCode(),
                        reason.getMessage(),
                        errors
                ));
    }

    /**
     * @Validated 유효성 검사 실패 처리 (RequestParam, PathVariable)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(
            ConstraintViolationException e,
            HttpServletRequest request
    ) {
        log.warn("[ConstraintViolation] Url: {}, Message: {}", request.getRequestURI(), e.getMessage());

        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("Invalid input");

        ErrorReasonDTO reason = ErrorStatus._BAD_REQUEST.getReasonHttpStatus();

        return ResponseEntity
                .status(reason.getHttpStatus())
                .body(ApiResponse.onFailure(
                        reason.getCode(),
                        reason.getMessage(),
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
        log.error("[Exception] Url: {}, Message: {}", request.getRequestURI(), e.getMessage(), e);

        ErrorReasonDTO reason = ErrorStatus._INTERNAL_SERVER_ERROR.getReasonHttpStatus();

        return ResponseEntity
                .status(reason.getHttpStatus())
                .body(ApiResponse.onFailure(
                        reason.getCode(),
                        reason.getMessage(),
                        e.getMessage()
                ));
    }
}