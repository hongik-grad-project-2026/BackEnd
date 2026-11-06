package com.mulmi.backend.domain.rental.dto.response;

import com.mulmi.backend.domain.rental.enums.RentalStatus;

import java.time.LocalDateTime;

// 명세 54번 성공 응답
public record RentalCheckoutResponseDTO(
        Long rentalId,
        Long itemId,
        String labelCode,
        RentalStatus status,
        LocalDateTime actualPickupAt,
        LocalDateTime expectedReturnAt
) {
}
