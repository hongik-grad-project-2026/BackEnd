package com.mulmi.backend.domain.rental.dto.response;

import com.mulmi.backend.domain.rental.enums.ApplicationType;
import com.mulmi.backend.domain.rental.enums.CancelReasonType;
import com.mulmi.backend.domain.rental.enums.RejectReasonType;
import com.mulmi.backend.domain.rental.enums.RentalStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 내 대여 목록(명세 47)의 한 줄
// status는 화면에 보여줄 상태 — RENTING이면서 반납 예정 시각이 지났으면 OVERDUE로 내려간다 (명세의 overdue 필드 대신)
// 반려 사유는 반려된 대여만 값이 있고, 상세 사유는 기타(OTHER)일 때만 있다
public record RentalSummaryResponseDTO(
        Long rentalId,
        Long modelId,
        String modelName,
        String modelImageUrl,           // S3 조회용 URL (DB에는 키가 저장됨)
        String categoryName,
        String labelCode,
        ApplicationType applicationType,
        RentalStatus status,
        LocalDate expectedPickupDate,
        LocalDateTime actualPickupAt,
        LocalDateTime expectedReturnAt,
        LocalDateTime actualReturnAt,
        RejectReasonType rejectReasonType,
        String rejectReasonDetail,
        CancelReasonType cancelReason,  // 학생이 취소했을 때만 (49)
        LocalDateTime canceledAt,
        LocalDateTime createdAt
) {
}
