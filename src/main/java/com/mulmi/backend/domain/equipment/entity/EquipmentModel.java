package com.mulmi.backend.domain.equipment.entity;

import com.mulmi.backend.domain.equipment.enums.ApplicationPolicy;
import com.mulmi.backend.domain.equipment.enums.EquipmentModelStatus;
import com.mulmi.backend.domain.equipment.enums.LoanPeriodType;
import com.mulmi.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "equipment_model", uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "model_name"}))
public class EquipmentModel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prefix_id", nullable = false)
    private LabelPrefix labelPrefix;

    @Column(name = "model_name", nullable = false, length = 100)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String components;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String specification;

    @Column(name = "model_image_url", nullable = true, length = 1000)
    private String modelImageUrl;

    @Column(name = "application_policy", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ApplicationPolicy applicationPolicy;

    @Column(name = "loan_period_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private LoanPeriodType loanPeriodType;

    @Column(name = "loan_duration_days", nullable = true)
    private Integer loanDurationDays;

    @Column(name = "pledge_required", nullable = false)
    private boolean pledgeRequired;

    @Column(name = "rental_status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private EquipmentModelStatus rentalStatus;

    @Column(name = "disposal_date", nullable = false)
    private LocalDate disposalDate;
}
