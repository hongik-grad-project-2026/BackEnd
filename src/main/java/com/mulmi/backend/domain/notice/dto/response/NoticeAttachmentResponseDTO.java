package com.mulmi.backend.domain.notice.dto.response;

import java.time.LocalDateTime;

public record NoticeAttachmentResponseDTO(
        Long attachmentId,
        String originalFileName,
        String contentType,
        long fileSize,
        String downloadUrl,
        LocalDateTime createdAt
) {
}
