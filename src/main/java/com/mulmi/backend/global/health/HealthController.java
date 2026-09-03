package com.mulmi.backend.global.health;

import com.mulmi.backend.global.apiPayload.ApiResponse;
import com.mulmi.backend.global.apiPayload.code.GeneralSuccessCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                new HealthResponse("UP")
        );
    }
}
