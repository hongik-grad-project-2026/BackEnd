package com.mulmi.backend.domain.notice.service;

import com.mulmi.backend.domain.notice.converter.NoticeConverter;
import com.mulmi.backend.domain.notice.dto.response.NoticeDetailResponseDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticePageResponseDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticeSummaryResponseDTO;
import com.mulmi.backend.domain.notice.entity.Notice;
import com.mulmi.backend.domain.notice.exception.NoticeException;
import com.mulmi.backend.domain.notice.exception.code.NoticeErrorCode;
import com.mulmi.backend.domain.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepository;

    @Override
    public NoticePageResponseDTO getNotices(Boolean important, int page, int size) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );
        Page<Notice> notices = noticeRepository.findNotices(important, pageable);
        Page<NoticeSummaryResponseDTO> responsePage = notices.map(
                NoticeConverter::toNoticeSummaryResponseDTO
        );

        return NoticePageResponseDTO.from(responsePage);
    }

    @Override
    public NoticeDetailResponseDTO getNotice(Long noticeId) {
        Notice notice = noticeRepository.findByIdAndDeletedAtIsNull(noticeId)
                .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));

        return NoticeConverter.toNoticeDetailResponseDTO(notice);
    }
}
