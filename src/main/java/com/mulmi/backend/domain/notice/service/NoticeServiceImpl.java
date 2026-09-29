package com.mulmi.backend.domain.notice.service;

import com.mulmi.backend.domain.notice.converter.NoticeConverter;
import com.mulmi.backend.domain.notice.dto.request.NoticeCreateRequestDTO;
import com.mulmi.backend.domain.notice.dto.request.NoticeUpdateRequestDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticeDetailResponseDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticePageResponseDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticeSummaryResponseDTO;
import com.mulmi.backend.domain.notice.entity.Notice;
import com.mulmi.backend.domain.notice.exception.NoticeException;
import com.mulmi.backend.domain.notice.exception.code.NoticeErrorCode;
import com.mulmi.backend.domain.notice.repository.NoticeRepository;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.domain.user.enums.UserStatus;
import com.mulmi.backend.domain.user.exception.UserException;
import com.mulmi.backend.domain.user.exception.code.UserErrorCode;
import com.mulmi.backend.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;

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

    @Override
    @Transactional
    public NoticeDetailResponseDTO createNotice(
            Long authorId,
            NoticeCreateRequestDTO dto
    ) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        if (author.getStatus() != UserStatus.ACTIVE) {
            throw new UserException(UserErrorCode.INACTIVE_USER);
        }

        Notice notice = NoticeConverter.toNotice(dto, author);
        Notice savedNotice = noticeRepository.save(notice);

        return NoticeConverter.toNoticeDetailResponseDTO(savedNotice);
    }

    @Override
    @Transactional
    public NoticeDetailResponseDTO updateNotice(
            Long noticeId,
            NoticeUpdateRequestDTO dto
    ) {
        Notice notice = noticeRepository.findByIdAndDeletedAtIsNull(noticeId)
                .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));

        notice.update(dto.title().trim(), dto.content().trim(), dto.important());

        return NoticeConverter.toNoticeDetailResponseDTO(notice);
    }
}
