package com.mulmi.backend.domain.user.dto.response;

public record StaffSummaryResponseDTO(
        Long userId,
        String loginId,
        String name
) {
}
