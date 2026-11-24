package com.mulmi.backend.domain.rental.enums;

// 대여 사진 방향 (ERD rental_image.image_side)
// 11-24 변경 : 수령·반납 사진을 종류별로 앞(FRONT)·뒤(BACK) 2장까지 받는다 (11-07엔 FRONT 1장만)
public enum RentalImageSide {
    FRONT,
    BACK
}
