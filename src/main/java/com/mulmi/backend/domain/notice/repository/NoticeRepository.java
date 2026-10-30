package com.mulmi.backend.domain.notice.repository;

import com.mulmi.backend.domain.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query(
            value = """
                    SELECT n
                    FROM Notice n
                    JOIN FETCH n.author
                    WHERE n.deletedAt IS NULL
                      AND (:important IS NULL OR n.important = :important)
                      AND (
                          :keyword IS NULL
                          OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          OR LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    """,
            countQuery = """
                    SELECT COUNT(n)
                    FROM Notice n
                    WHERE n.deletedAt IS NULL
                      AND (:important IS NULL OR n.important = :important)
                      AND (
                          :keyword IS NULL
                          OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          OR LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    """
    )
    Page<Notice> findNotices(
            @Param("keyword") String keyword,
            @Param("important") Boolean important,
            Pageable pageable
    );

    Optional<Notice> findByIdAndDeletedAtIsNull(Long noticeId);
}
