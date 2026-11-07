package com.mulmi.backend.domain.rental.dto.response;

import com.mulmi.backend.domain.rental.enums.ApplicationType;
import com.mulmi.backend.domain.rental.enums.RejectReasonType;
import com.mulmi.backend.domain.rental.enums.RentalStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 내 대여 상세(명세 48)
// status는 화면에 보여줄 상태 — 연체면 OVERDUE (명세의 overdue 필드 대신, 47과 같음)
// 사진은 학생도 본다 : 반납 때 파손이 있으면 수령 당시 사진이 책임을 가리는 근거가 된다 (11-07 결정)
// 서약서는 업로드 기능이 정해지면 추가 (명세의 pledgeAgreed는 동의 화면이 없어 만들지 않음)
public record RentalDetailResponseDTO(
        Long rentalId,
        Long modelId,
        String modelName,
        String modelImageUrl,           // S3 조회용 URL (DB에는 키가 저장됨)
        String modelDescription,
        String components,
        String categoryName,
        String labelCode,
        ApplicationType applicationType,
        RentalStatus status,
        String purpose,
        String memo,
        LocalDate expectedPickupDate,
        LocalDateTime reviewedAt,       // 승인·반려 시각
        LocalDateTime actualPickupAt,
        LocalDateTime expectedReturnAt,
        LocalDateTime actualReturnAt,
        RejectReasonType rejectReasonType,
        String rejectReasonDetail,
        String pickupImageUrl,          // 수령 당시 사진 (없으면 null)
        String returnImageUrl,          // 반납 당시 사진 (없으면 null)
        LocalDateTime createdAt
) {
}
