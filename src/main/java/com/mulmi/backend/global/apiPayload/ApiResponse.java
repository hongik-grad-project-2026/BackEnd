package com.mulmi.backend.global.apiPayload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.mulmi.backend.global.code.BaseCode;
import com.mulmi.backend.global.status.SuccessStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class ApiResponse<T> {

    @JsonProperty("isSuccess")
    private final Boolean isSuccess;

    private final String code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T result;

    //성공한 경우의 응답
    public static <T> ApiResponse<T> onSuccess(T result){
        return new ApiResponse<T>(
                true,
                SuccessStatus._OK.getCode(),
                SuccessStatus._OK.getMessage(),
                result
        );
    }

    // 전달받은 BaseCode의 코드/메시지를 사용해 성공 응답을 생성
    public static <T> ApiResponse<T> of(BaseCode code, T result) {
        return new ApiResponse<>(
                true,
                code.getReasonHttpStatus().getCode(),
                code.getReasonHttpStatus().getMessage(),
                result
        );
    }

    //싫패한 경우의 응답
    public static <T> ApiResponse<T> onFailure(String code, String message, T result) {
        return new ApiResponse<>(
                false,
                code,
                message,
                result
        );
    }

}
