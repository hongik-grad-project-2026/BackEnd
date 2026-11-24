package com.mulmi.backend.domain.notice.repository;

import com.mulmi.backend.domain.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // 동시에 상세 조회해도 증가분을 잃지 않도록 DB에서 한 문장으로 더한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Notice n
            SET n.viewCount = n.viewCount + 1
            WHERE n.id = :noticeId
              AND n.deletedAt IS NULL
            """)
    int incrementViewCount(@Param("noticeId") Long noticeId);
}
