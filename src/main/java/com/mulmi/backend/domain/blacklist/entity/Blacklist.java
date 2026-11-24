package com.mulmi.backend.domain.blacklist.entity;

import com.mulmi.backend.domain.blacklist.enums.BlacklistReasonType;
import com.mulmi.backend.domain.rental.entity.Rental;
import com.mulmi.backend.domain.semester.entity.Semester;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "blacklists",
        indexes = @Index(
                name = "idx_blacklists_user_semester_release",
                columnList = "user_id,semester_id,released_at"
        ),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_blacklists_rental_reason",
                columnNames = {"rental_id", "reason_type"}
        )
)
public class Blacklist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 제재가 적용되는 학기. 회원 상태와 분리하여 해당 학기의 대여만 제한한다.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    // 노쇼·연체를 발생시킨 대여. 관리자 수동 등록이면 null이다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id")
    private Rental rental;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", nullable = false, length = 30)
    private BlacklistReasonType reasonType;

    @Column(name = "reason_detail", columnDefinition = "TEXT")
    private String reasonDetail;

    // 자동 등록이면 null, 관리자가 직접 등록했으면 해당 관리자이다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by")
    private User registeredBy;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "released_by")
    private User releasedBy;

    @Column(name = "release_reason", columnDefinition = "TEXT")
    private String releaseReason;

    public boolean isActive() {
        return releasedAt == null;
    }

    public void update(Semester semester, String reasonDetail) {
        this.semester = semester;
        this.reasonDetail = reasonDetail;
    }

    public void release(User administrator, String releaseReason, LocalDateTime now) {
        this.releasedBy = administrator;
        this.releaseReason = releaseReason;
        this.releasedAt = now;
    }
}
