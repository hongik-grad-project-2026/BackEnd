package com.mulmi.backend.domain.notice.service;

import com.mulmi.backend.domain.notice.dto.response.NoticePageResponseDTO;

public interface NoticeService {

    NoticePageResponseDTO getNotices(Boolean important, int page, int size);
}
