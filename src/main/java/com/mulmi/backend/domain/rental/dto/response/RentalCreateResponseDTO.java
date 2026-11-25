package com.mulmi.backend.domain.rental.dto.response;

import com.mulmi.backend.domain.rental.enums.ApplicationType;
import com.mulmi.backend.domain.rental.enums.RentalStatus;

import java.time.LocalDate;

// 명세 46번 성공 응답. 수령희망일은 11-03 결정으로 날짜만 (명세의 expectedPickupAt → expectedPickupDate)
//  snack~ 칸 : 졸업전시 간식일 때만 값이 있다 (명세 64). 일반 기자재는 모두 null
public record RentalCreateResponseDTO(
        Long rentalId,
        RentalStatus status,
        ApplicationType applicationType,
        LocalDate expectedPickupDate,
        Long snackModelId,      // 실제로 받은 간식 모델 (랜덤이면 당첨 경품, 누른 카드와 다르다)
        String snackName,       // 등수 표시([2등])를 뗀 이름
        String rank,            // 1등 / 2등 / 3등 / 꽝, 일반 간식은 null
        String imageUrl         // 받은 간식 이미지, 없으면 null
) {
    // 일반 기자재 신청 : 간식 칸 없이
    public RentalCreateResponseDTO(Long rentalId, RentalStatus status,
                                   ApplicationType applicationType, LocalDate expectedPickupDate) {
        this(rentalId, status, applicationType, expectedPickupDate, null, null, null, null);
    }
}
