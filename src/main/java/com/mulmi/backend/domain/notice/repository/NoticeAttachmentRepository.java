package com.mulmi.backend.domain.notice.repository;

import com.mulmi.backend.domain.notice.entity.NoticeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {

    List<NoticeAttachment> findAllByNoticeIdOrderByIdAsc(Long noticeId);

    Optional<NoticeAttachment> findByIdAndNoticeId(Long attachmentId, Long noticeId);

    @Query("""
            SELECT DISTINCT attachment.notice.id
            FROM NoticeAttachment attachment
            WHERE attachment.notice.id IN :noticeIds
            """)
    Set<Long> findNoticeIdsWithAttachments(@Param("noticeIds") Collection<Long> noticeIds);
}
