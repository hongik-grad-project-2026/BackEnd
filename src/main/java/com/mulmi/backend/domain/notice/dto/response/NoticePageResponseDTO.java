package com.mulmi.backend.domain.notice.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record NoticePageResponseDTO(
        List<NoticeSummaryResponseDTO> notices,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static NoticePageResponseDTO from(Page<NoticeSummaryResponseDTO> page) {
        return new NoticePageResponseDTO(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
