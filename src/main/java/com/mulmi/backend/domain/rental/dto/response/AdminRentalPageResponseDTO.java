package com.mulmi.backend.domain.rental.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

// 명세 50번 응답 : 목록 키는 명세대로 content, 탭 옆 숫자는 statusCounts (11-07 결정 : 목록 응답에 함께)
public record AdminRentalPageResponseDTO(
        List<AdminRentalSummaryResponseDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        Map<String, Long> statusCounts
) {
    public static AdminRentalPageResponseDTO of(
            Page<AdminRentalSummaryResponseDTO> page,
            Map<String, Long> statusCounts
    ) {
        return new AdminRentalPageResponseDTO(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                statusCounts
        );
    }
}
