package com.mulmi.backend.domain.rental.dto.response;

import com.mulmi.backend.domain.rental.enums.ApplicationType;
import com.mulmi.backend.domain.rental.enums.RentalStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 관리자 대여 목록의 한 줄 : 승인·현황·기록 세 화면이 쓰는 항목을 합친 것
// status는 화면에 보여줄 상태 — RENTING이면서 반납 예정 시각이 지났으면 OVERDUE로 내려간다
public record AdminRentalSummaryResponseDTO(
        Long rentalId,
        Long userId,
        String userName,
        String studentId,
        String department,
        String categoryName,
        Long modelId,
        String modelName,
        String labelCode,
        ApplicationType applicationType,
        RentalStatus status,
        String purpose,
        LocalDate expectedPickupDate,
        LocalDateTime expectedReturnAt,
        LocalDateTime actualPickupAt,   // 대여현황 화면의 "수령일" (54번에서 기록)
        LocalDateTime createdAt
) {
}
