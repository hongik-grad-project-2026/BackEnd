package com.mulmi.backend.domain.rental.service;

import com.mulmi.backend.domain.rental.dto.request.RentalCreateRequestDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalCreateResponseDTO;

public interface RentalService {

    // 대여 신청 (명세 46번)
    RentalCreateResponseDTO createRental(Long userId, RentalCreateRequestDTO dto);
}
