package com.mulmi.backend.domain.notice.converter;

import com.mulmi.backend.domain.notice.dto.response.NoticeSummaryResponseDTO;
import com.mulmi.backend.domain.notice.entity.Notice;

public class NoticeConverter {

    private NoticeConverter() {
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
}
