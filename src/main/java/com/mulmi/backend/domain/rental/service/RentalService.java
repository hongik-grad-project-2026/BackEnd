package com.mulmi.backend.domain.rental.service;

import com.mulmi.backend.domain.rental.dto.request.RentalCancelRequestDTO;
import com.mulmi.backend.domain.rental.dto.request.RentalCreateRequestDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalCancelResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalCreateResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalDetailResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalPageResponseDTO;
import com.mulmi.backend.domain.rental.enums.RentalStatus;

import java.util.List;

public interface RentalService {

    // 대여 신청 (명세 46번)
    RentalCreateResponseDTO createRental(Long userId, RentalCreateRequestDTO dto);

    // 내 대여 목록 (명세 47번)
    RentalPageResponseDTO getMyRentals(Long userId, List<RentalStatus> statuses, String keyword, int page, int size);

    // 내 대여 상세 (명세 48번)
    RentalDetailResponseDTO getMyRental(Long userId, Long rentalId);

    // 신청 취소 (명세 49번)
    RentalCancelResponseDTO cancelRental(Long userId, Long rentalId, RentalCancelRequestDTO dto);
}
