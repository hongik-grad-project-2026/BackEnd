package com.mulmi.backend.domain.rental.enums;

// 대여 사진 종류 (ERD rental_image.image_type)
public enum RentalImageType {
    PICKUP, // 수령 당시 (대여 상태 READY_FOR_PICKUP에서 등록)
    RETURN  // 반납 당시 (대여 상태 RENTING에서 등록)
}
