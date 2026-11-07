package com.mulmi.backend.domain.rental.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

// 명세 47번 응답 : 50과 같은 모양 (목록 키 content + 탭 옆 숫자 statusCounts)
// statusCounts의 RENTING에는 연체가 빠져 있고 OVERDUE에 따로 들어간다 → 대여중 탭 숫자는 RENTING + OVERDUE
public record RentalPageResponseDTO(
        List<RentalSummaryResponseDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        Map<String, Long> statusCounts
) {
    public static RentalPageResponseDTO of(
            Page<RentalSummaryResponseDTO> page,
            Map<String, Long> statusCounts
    ) {
        return new RentalPageResponseDTO(
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
