package com.mulmi.backend.domain.rental.dto.response;

import com.mulmi.backend.domain.rental.enums.RejectReasonType;
import com.mulmi.backend.domain.rental.enums.RentalStatus;

// 명세 53번 성공 응답
public record RentalRejectResponseDTO(
        Long rentalId,
        RentalStatus status,
        RejectReasonType rejectReasonType,
        String rejectReasonDetail
) {
}
