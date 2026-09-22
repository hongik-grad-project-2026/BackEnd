package com.mulmi.backend.domain.notice.service;

import com.mulmi.backend.domain.notice.converter.NoticeConverter;
import com.mulmi.backend.domain.notice.dto.response.NoticePageResponseDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticeSummaryResponseDTO;
import com.mulmi.backend.domain.notice.entity.Notice;
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
}
