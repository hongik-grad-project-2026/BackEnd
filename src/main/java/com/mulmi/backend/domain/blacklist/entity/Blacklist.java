package com.mulmi.backend.domain.blacklist.entity;

import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "blacklists",
        indexes = @Index(
                name = "idx_blacklists_user_period",
                columnList = "user_id,start_date,end_date"
        )
)
public class Blacklist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Rental 엔터티 구현 후 연관관계로 전환한다.
    @Column(name = "rental_id")
    private Long rentalId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    public void updatePeriodAndReason(
            LocalDate startDate,
            LocalDate endDate,
            String reason
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
    }

    public boolean overlaps(LocalDate otherStartDate, LocalDate otherEndDate) {
        return !endDate.isBefore(otherStartDate)
                && !startDate.isAfter(otherEndDate);
    }
}
