package com.mulmi.backend.domain.rental.dto.request;

import com.mulmi.backend.domain.rental.enums.CancelReasonType;
import jakarta.validation.constraints.NotNull;

// 명세 49번 요청 : 명세는 본문 없음이었으나 화면이 취소 사유 선택을 필수로 받아 추가 (11-07 결정)
public record RentalCancelRequestDTO(
        @NotNull(message = "취소 사유는 필수입니다.")
        CancelReasonType cancelReason
) {
}
