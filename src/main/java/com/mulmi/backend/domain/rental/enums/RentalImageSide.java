package com.mulmi.backend.domain.rental.enums;

// 대여 사진 방향 (ERD rental_image.image_side)
// 11-07 결정 : 사진은 1장(FRONT)만 받는다. BACK은 나중에 뒷면이 필요해지면 쓰려고 ERD대로 남겨 둔다
public enum RentalImageSide {
    FRONT,
    BACK
}
