package com.mulmi.backend.global.apiPayload.exception;

import com.mulmi.backend.global.apiPayload.ApiResponse;
import com.mulmi.backend.global.apiPayload.code.GeneralErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

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
     * 스프링 MVC 표준 예외 처리 (405, 415, 타입 변환 실패, 깨진 JSON, 404 등)
     * 부모 클래스가 예외별로 상태 코드를 정한 뒤 이 메서드로 모은다.
     * 여기서 응답 본문만 ApiResponse 형식으로 바꾼다.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception e,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        log.warn("[{}] {}, Message: {}",
                e.getClass().getSimpleName(),
                request.getDescription(false),
                e.getMessage()
        );

        GeneralErrorCode code = toErrorCode(statusCode);

        return ResponseEntity
                .status(statusCode)
                .headers(headers)
                .body(ApiResponse.onFailure(code, null));
    }

    /**
     * @Valid 유효성 검사 실패 처리 RequestBody
     * 부모 클래스가 이미 이 예외를 처리하므로 @ExceptionHandler가 아닌 오버라이드로 둔다.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.warn("[MethodArgumentNotValid] {}, Message: {}",
                request.getDescription(false),
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
     * @PreAuthorize 권한 부족 처리
     * Controller 호출 중에 발생하므로 시큐리티 필터가 아닌 여기서 잡힌다.
     * 이 핸들러가 없으면 아래 Exception 핸들러에 걸려 500으로 나간다.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(
            AccessDeniedException e,
            HttpServletRequest request
    ) {
        log.warn("[AccessDenied] Url: {}, Message: {}",
                request.getRequestURI(),
                e.getMessage()
        );

        return ResponseEntity
                .status(GeneralErrorCode.FORBIDDEN.getStatus())
                .body(ApiResponse.onFailure(GeneralErrorCode.FORBIDDEN, null));
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

        // 내부 정보(클래스명, SQL 등)가 새지 않도록 메시지는 로그에만 남긴다.
        return ResponseEntity
                .status(GeneralErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.onFailure(GeneralErrorCode.INTERNAL_SERVER_ERROR, null));
    }

    // 상태 코드에 맞는 GeneralErrorCode를 찾는다. 없으면 4xx는 BAD_REQUEST, 그 외는 INTERNAL_SERVER_ERROR.
    private GeneralErrorCode toErrorCode(HttpStatusCode statusCode) {
        return Arrays.stream(GeneralErrorCode.values())
                .filter(code -> code.getStatus().value() == statusCode.value())
                .findFirst()
                .orElse(statusCode.is4xxClientError()
                        ? GeneralErrorCode.BAD_REQUEST
                        : GeneralErrorCode.INTERNAL_SERVER_ERROR);
    }
}