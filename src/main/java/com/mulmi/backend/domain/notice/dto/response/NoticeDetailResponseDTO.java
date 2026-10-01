package com.mulmi.backend.domain.notice.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record NoticeDetailResponseDTO(
        Long noticeId,
        String title,
        String content,
        boolean important,
        Long authorId,
        String authorName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<NoticeAttachmentResponseDTO> attachments
) {
}
