package com.mulmi.backend.global.health;

import com.mulmi.backend.global.apiPayload.ApiResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTest {

    private final HealthController healthController = new HealthController();

    @Test
    void returnsUpStatus() {
        ApiResponse<HealthResponse> response = healthController.health();

        assertThat(response.getIsSuccess()).isTrue();
        assertThat(response.getCode()).isEqualTo("COMMON200_1");
        assertThat(response.getResult().status()).isEqualTo("UP");
    }
}
