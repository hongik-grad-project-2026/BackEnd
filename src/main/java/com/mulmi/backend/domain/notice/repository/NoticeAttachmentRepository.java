package com.mulmi.backend.domain.notice.repository;

import com.mulmi.backend.domain.notice.entity.NoticeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {

    List<NoticeAttachment> findAllByNoticeIdOrderByIdAsc(Long noticeId);

    Optional<NoticeAttachment> findByIdAndNoticeId(Long attachmentId, Long noticeId);
}
