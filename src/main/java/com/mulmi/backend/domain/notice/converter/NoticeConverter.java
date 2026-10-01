package com.mulmi.backend.domain.notice.converter;

import com.mulmi.backend.domain.notice.dto.request.NoticeCreateRequestDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticeDetailResponseDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticeAttachmentResponseDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticeSummaryResponseDTO;
import com.mulmi.backend.domain.notice.entity.Notice;
import com.mulmi.backend.domain.notice.entity.NoticeAttachment;
import com.mulmi.backend.domain.user.entity.User;

import java.util.List;

public class NoticeConverter {

    private NoticeConverter() {
    }

    public static Notice toNotice(NoticeCreateRequestDTO dto, User author) {
        return Notice.builder()
                .title(dto.title().trim())
                .content(dto.content().trim())
                .important(dto.important())
                .author(author)
                .build();
    }

    public static NoticeSummaryResponseDTO toNoticeSummaryResponseDTO(Notice notice) {
        return new NoticeSummaryResponseDTO(
                notice.getId(),
                notice.getTitle(),
                notice.isImportant(),
                notice.getAuthor().getId(),
                notice.getAuthor().getName(),
                notice.getCreatedAt()
        );
    }

    public static NoticeDetailResponseDTO toNoticeDetailResponseDTO(
            Notice notice,
            List<NoticeAttachmentResponseDTO> attachments
    ) {
        return new NoticeDetailResponseDTO(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.isImportant(),
                notice.getAuthor().getId(),
                notice.getAuthor().getName(),
                notice.getCreatedAt(),
                notice.getUpdatedAt(),
                attachments
        );
    }

    public static NoticeAttachmentResponseDTO toNoticeAttachmentResponseDTO(
            NoticeAttachment attachment
    ) {
        return new NoticeAttachmentResponseDTO(
                attachment.getId(),
                attachment.getOriginalFileName(),
                attachment.getContentType(),
                attachment.getFileSize(),
                "/api/notices/" + attachment.getNotice().getId()
                        + "/attachments/" + attachment.getId() + "/download",
                attachment.getCreatedAt()
        );
    }
}
