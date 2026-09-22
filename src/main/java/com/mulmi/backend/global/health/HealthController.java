package com.mulmi.backend.global.health;

import com.mulmi.backend.global.apiPayload.ApiResponse;
import com.mulmi.backend.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@Tag(name = "서버 상태", description = "서버 상태 확인 API")
public class HealthController {

    @Operation(summary = "서버 상태 확인")
    @GetMapping
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                new HealthResponse("UP")
        );
    }
}
