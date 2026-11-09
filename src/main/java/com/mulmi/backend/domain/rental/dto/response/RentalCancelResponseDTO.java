package com.mulmi.backend.domain.rental.dto.response;

import com.mulmi.backend.domain.rental.enums.CancelReasonType;
import com.mulmi.backend.domain.rental.enums.RentalStatus;

import java.time.LocalDateTime;

// 명세 49번 성공 응답 (명세의 rentalId·status + 취소 사유·시각)
public record RentalCancelResponseDTO(
        Long rentalId,
        RentalStatus status,
        CancelReasonType cancelReason,
        LocalDateTime canceledAt
) {
}
