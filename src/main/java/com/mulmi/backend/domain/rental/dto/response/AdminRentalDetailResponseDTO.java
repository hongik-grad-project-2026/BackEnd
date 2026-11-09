package com.mulmi.backend.domain.rental.dto.response;

import com.mulmi.backend.domain.rental.enums.ApplicationType;
import com.mulmi.backend.domain.rental.enums.CancelReasonType;
import com.mulmi.backend.domain.rental.enums.RejectReasonType;
import com.mulmi.backend.domain.rental.enums.RentalStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// 관리자 대여 상세(명세 51) : 명세처럼 신청자(user)·모델(model)을 묶어서 내려준다
// 명세와 다른 점 : pledgeAgreed 없음(서약서는 수령 사진에 포함), overdue 대신 status=OVERDUE,
//                 화면용 이메일·학과·설명·카테고리, 49의 취소 사유·시각 추가
public record AdminRentalDetailResponseDTO(
        Long rentalId,
        UserInfo user,
        ModelInfo model,
        Long itemId,
        String labelCode,
        ApplicationType applicationType,
        RentalStatus status,
        String purpose,
        String memo,
        LocalDate expectedPickupDate,
        LocalDateTime actualPickupAt,
        LocalDateTime expectedReturnAt,
        LocalDateTime actualReturnAt,
        String reviewedBy,              // 승인·반려한 조교 이름 (처리 전이면 null)
        LocalDateTime reviewedAt,
        String pickupCheckedBy,         // 수령 처리한 사람 이름
        String returnCheckedBy,         // 반납 처리한 사람 이름
        RejectReasonType rejectReasonType,
        String rejectReasonDetail,
        CancelReasonType cancelReason,
        LocalDateTime canceledAt,
        List<RentalImageResponseDTO> images,
        LocalDateTime createdAt
) {
    public record UserInfo(
            Long userId,
            String name,
            String studentId,
            String email,
            String phone,
            String department
    ) {
    }

    public record ModelInfo(
            Long modelId,
            String modelName,
            String categoryName,
            String description,
            String components
    ) {
    }
}
