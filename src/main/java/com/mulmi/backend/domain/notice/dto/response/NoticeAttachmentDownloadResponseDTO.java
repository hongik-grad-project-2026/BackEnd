package com.mulmi.backend.domain.notice.dto.response;

public record NoticeAttachmentDownloadResponseDTO(
        String originalFileName,
        String contentType,
        byte[] content
) {
}
