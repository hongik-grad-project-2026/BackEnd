package com.mulmi.backend.domain.rental.dto.response;

import com.mulmi.backend.domain.rental.enums.RentalStatus;

import java.time.LocalDateTime;

// 명세 52번 성공 응답
public record RentalApproveResponseDTO(
        Long rentalId,
        RentalStatus status,
        LocalDateTime reviewedAt
) {
}
