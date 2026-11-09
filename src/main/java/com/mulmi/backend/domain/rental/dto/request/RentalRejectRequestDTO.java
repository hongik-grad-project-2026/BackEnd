package com.mulmi.backend.domain.rental.dto.request;

import com.mulmi.backend.domain.rental.enums.RejectReasonType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 명세 53번 요청. 기타(OTHER)일 때 상세 사유 필수 여부는 다른 필드 값에 따라 달라서 서비스에서 검사한다
public record RentalRejectRequestDTO(
        @NotNull(message = "반려 사유는 필수입니다.")
        RejectReasonType rejectReasonType,

        @Size(max = 500, message = "상세 사유는 500자를 넘을 수 없습니다.")
        String rejectReasonDetail
) {
}
