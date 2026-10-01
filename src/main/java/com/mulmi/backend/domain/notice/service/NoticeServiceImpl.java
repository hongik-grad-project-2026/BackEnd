package com.mulmi.backend.domain.notice.service;

import com.mulmi.backend.domain.notice.converter.NoticeConverter;
import com.mulmi.backend.domain.notice.dto.request.NoticeCreateRequestDTO;
import com.mulmi.backend.domain.notice.dto.request.NoticeUpdateRequestDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticeDetailResponseDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticeAttachmentResponseDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticePageResponseDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticeSummaryResponseDTO;
import com.mulmi.backend.domain.notice.entity.Notice;
import com.mulmi.backend.domain.notice.entity.NoticeAttachment;
import com.mulmi.backend.domain.notice.exception.NoticeException;
import com.mulmi.backend.domain.notice.exception.code.NoticeErrorCode;
import com.mulmi.backend.domain.notice.repository.NoticeRepository;
import com.mulmi.backend.domain.notice.repository.NoticeAttachmentRepository;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.domain.user.enums.UserStatus;
import com.mulmi.backend.domain.user.exception.UserException;
import com.mulmi.backend.domain.user.exception.code.UserErrorCode;
import com.mulmi.backend.domain.user.repository.UserRepository;
import com.mulmi.backend.global.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeServiceImpl implements NoticeService {

    private static final long MAX_ATTACHMENT_SIZE = 20 * 1024 * 1024;
    private static final Set<String> ATTACHMENT_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final NoticeRepository noticeRepository;
    private final NoticeAttachmentRepository noticeAttachmentRepository;
    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;

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

        return toNoticeDetailResponseDTO(notice);
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

        return toNoticeDetailResponseDTO(savedNotice);
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

        return toNoticeDetailResponseDTO(notice);
    }

    @Override
    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = noticeRepository.findByIdAndDeletedAtIsNull(noticeId)
                .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));

        notice.delete();
    }

    @Override
    @Transactional
    public NoticeAttachmentResponseDTO uploadAttachment(
            Long noticeId,
            MultipartFile file
    ) {
        Notice notice = findNotice(noticeId);
        String originalFileName = validateAttachment(file);
        String objectKey = s3StorageService.upload(
                file,
                "notices/" + noticeId + "/attachments"
        );

        NoticeAttachment attachment = NoticeAttachment.builder()
                .notice(notice)
                .originalFileName(originalFileName)
                .storedFileName(extractStoredFileName(objectKey))
                .fileUrl(objectKey)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .build();

        try {
            NoticeAttachment savedAttachment = noticeAttachmentRepository.save(attachment);
            return NoticeConverter.toNoticeAttachmentResponseDTO(savedAttachment);
        } catch (RuntimeException exception) {
            s3StorageService.delete(objectKey);
            throw exception;
        }
    }

    private Notice findNotice(Long noticeId) {
        return noticeRepository.findByIdAndDeletedAtIsNull(noticeId)
                .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));
    }

    private NoticeDetailResponseDTO toNoticeDetailResponseDTO(Notice notice) {
        List<NoticeAttachmentResponseDTO> attachments = noticeAttachmentRepository
                .findAllByNoticeIdOrderByIdAsc(notice.getId())
                .stream()
                .map(NoticeConverter::toNoticeAttachmentResponseDTO)
                .toList();
        return NoticeConverter.toNoticeDetailResponseDTO(notice, attachments);
    }

    private String validateAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()
                || !ATTACHMENT_CONTENT_TYPES.contains(file.getContentType())) {
            throw new NoticeException(NoticeErrorCode.INVALID_ATTACHMENT);
        }
        if (file.getSize() > MAX_ATTACHMENT_SIZE) {
            throw new NoticeException(NoticeErrorCode.ATTACHMENT_TOO_LARGE);
        }

        try {
            String originalFileName = Path.of(file.getOriginalFilename()).getFileName().toString();
            if (originalFileName.isBlank() || originalFileName.length() > 255) {
                throw new NoticeException(NoticeErrorCode.INVALID_ATTACHMENT_NAME);
            }
            return originalFileName;
        } catch (InvalidPathException | NullPointerException exception) {
            throw new NoticeException(NoticeErrorCode.INVALID_ATTACHMENT_NAME);
        }
    }

    private String extractStoredFileName(String objectKey) {
        int separatorIndex = objectKey.lastIndexOf('/');
        return separatorIndex >= 0 ? objectKey.substring(separatorIndex + 1) : objectKey;
    }
}
