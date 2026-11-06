package com.mulmi.backend.domain.rental.service;

import com.mulmi.backend.domain.rental.dto.response.RentalApproveResponseDTO;

public interface AdminRentalService {

    // 대여 승인 (명세 52번)
    RentalApproveResponseDTO approveRental(Long rentalId, Long assistantId);
}
