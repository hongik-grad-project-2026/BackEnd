package com.mulmi.backend.domain.notice.dto.response;

import java.time.LocalDateTime;

public record NoticeSummaryResponseDTO(
        Long noticeId,
        String title,
        boolean important,
        Long authorId,
        String authorName,
        LocalDateTime createdAt
) {
}
