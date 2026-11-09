package com.mulmi.backend.domain.rental.dto.response;

import com.mulmi.backend.domain.rental.enums.RentalImageType;

import java.time.LocalDateTime;

// 명세 55번 성공 응답. imageUrl은 S3 임시 조회 URL
// 교체한 경우 처음 등록 시각이 아니라 이번에 올린 시각이 의미 있으므로 uploadedAt으로 둔다
public record RentalImageUploadResponseDTO(
        Long imageId,
        Long rentalId,
        RentalImageType imageType,
        String imageUrl,
        LocalDateTime uploadedAt
) {
}
