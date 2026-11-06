package com.mulmi.backend.domain.rental.dto.response;

import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;
import com.mulmi.backend.domain.rental.enums.RentalStatus;

import java.time.LocalDateTime;

// 명세 57번 성공 응답. wasOverdue : 반납 시점에 반납 예정 시각이 지났는지 (연체는 저장하지 않고 계산)
public record RentalReturnResponseDTO(
        Long rentalId,
        Long itemId,
        RentalStatus status,
        EquipmentItemStatus itemStatus,
        LocalDateTime actualReturnAt,
        boolean wasOverdue
) {
}
