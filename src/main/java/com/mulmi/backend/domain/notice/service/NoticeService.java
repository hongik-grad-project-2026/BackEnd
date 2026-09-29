package com.mulmi.backend.domain.notice.service;

import com.mulmi.backend.domain.notice.dto.request.NoticeCreateRequestDTO;
import com.mulmi.backend.domain.notice.dto.request.NoticeUpdateRequestDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticeDetailResponseDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticePageResponseDTO;

public interface NoticeService {

    NoticePageResponseDTO getNotices(Boolean important, int page, int size);

    NoticeDetailResponseDTO getNotice(Long noticeId);

    NoticeDetailResponseDTO createNotice(Long authorId, NoticeCreateRequestDTO dto);

    NoticeDetailResponseDTO updateNotice(Long noticeId, NoticeUpdateRequestDTO dto);

    void deleteNotice(Long noticeId);
}
