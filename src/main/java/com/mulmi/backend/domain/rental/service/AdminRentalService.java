package com.mulmi.backend.domain.rental.service;

import com.mulmi.backend.domain.rental.dto.request.RentalRejectRequestDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalApproveResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalRejectResponseDTO;

public interface AdminRentalService {

    // 대여 승인 (명세 52번)
    RentalApproveResponseDTO approveRental(Long rentalId, Long assistantId);

    // 대여 반려 (명세 53번)
    RentalRejectResponseDTO rejectRental(Long rentalId, Long assistantId, RentalRejectRequestDTO dto);
}
