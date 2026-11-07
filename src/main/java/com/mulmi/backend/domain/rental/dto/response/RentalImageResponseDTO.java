package com.mulmi.backend.domain.rental.dto.response;

import com.mulmi.backend.domain.rental.enums.RentalImageSide;
import com.mulmi.backend.domain.rental.enums.RentalImageType;

import java.time.LocalDateTime;

// 대여 사진 한 장 (명세 51 상세의 images, 56 사진 조회가 같은 형식)
// uploadedAt : 교체됐으면 마지막으로 올린 시각 (55 업로드 응답과 같은 이름)
public record RentalImageResponseDTO(
        Long imageId,
        RentalImageType imageType,
        RentalImageSide imageSide,
        String imageUrl,                // S3 임시 조회 URL
        LocalDateTime uploadedAt
) {
}
