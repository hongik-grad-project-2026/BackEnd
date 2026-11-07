package com.mulmi.backend.domain.rental.dto.response;

import java.util.List;

// 명세 56번 성공 응답 : 사진 한 장의 형식은 51 상세의 images와 같다
public record RentalImageListResponseDTO(
        Long rentalId,
        List<RentalImageResponseDTO> images
) {
}
