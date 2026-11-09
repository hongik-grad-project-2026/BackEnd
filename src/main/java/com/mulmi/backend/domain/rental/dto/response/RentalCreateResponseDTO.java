package com.mulmi.backend.domain.rental.dto.response;

import com.mulmi.backend.domain.rental.enums.ApplicationType;
import com.mulmi.backend.domain.rental.enums.RentalStatus;

import java.time.LocalDate;

// 명세 46번 성공 응답. 수령희망일은 11-03 결정으로 날짜만 (명세의 expectedPickupAt → expectedPickupDate)
public record RentalCreateResponseDTO(
        Long rentalId,
        RentalStatus status,
        ApplicationType applicationType,
        LocalDate expectedPickupDate
) {
}
