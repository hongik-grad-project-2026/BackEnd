package com.mulmi.backend.domain.notice.repository;

import com.mulmi.backend.domain.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query(
            value = """
                    SELECT n
                    FROM Notice n
                    JOIN FETCH n.author
                    WHERE n.deletedAt IS NULL
                      AND (:important IS NULL OR n.important = :important)
                    """,
            countQuery = """
                    SELECT COUNT(n)
                    FROM Notice n
                    WHERE n.deletedAt IS NULL
                      AND (:important IS NULL OR n.important = :important)
                    """
    )
    Page<Notice> findNotices(
            @Param("important") Boolean important,
            Pageable pageable
    );
}
