package com.mulmi.backend.domain.rental.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// 학기·신청 방식(사전/상시)은 받지 않는다 — 서버가 신청 시각으로 정한다
public record RentalCreateRequestDTO(
        @NotNull(message = "모델은 필수입니다.")
        Long modelId,

        @NotBlank(message = "대여 목적은 필수입니다.")
        @Size(max = 500, message = "대여 목적은 500자를 넘을 수 없습니다.")
        String purpose,

        @NotNull(message = "수령희망일은 필수입니다.")
        LocalDate expectedPickupDate,

        @Size(max = 500, message = "메모는 500자를 넘을 수 없습니다.")
        String memo
) {
}
