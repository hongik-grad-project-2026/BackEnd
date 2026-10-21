package com.mulmi.backend.domain.equipment.entity;

import com.mulmi.backend.domain.equipment.enums.LoanPeriodType;
import com.mulmi.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "category")
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name= "category_name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "label_required", nullable = false)
    private boolean labelRequired;

    // 정기대여 가능 여부
    @Column(name = "regular_rental_enabled", nullable = false)
    private boolean regularRentalEnabled;

    // 기간 유형
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "loan_period_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private LoanPeriodType loanPeriodType;

    // 대여 일수 (SEMESTER_END면 null)
    @Column(name = "loan_duration_days", nullable = true)
    private Integer loanDurationDays;

    // 수령 사진 필수 여부
    @Column(name = "pickup_photo_required", nullable = false)
    private boolean pickupPhotoRequired;

    // 서약서 필수 여부
    @Column(name = "pledge_required", nullable = false)
    private boolean pledgeRequired;

    public void updateCategoryName(String name) {
        this.name = name;
    }

    public void restore() {
        markRestored();
    }

    public void delete() {
        markDeleted();
    }

}
