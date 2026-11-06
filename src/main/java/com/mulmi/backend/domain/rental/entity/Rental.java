package com.mulmi.backend.domain.rental.entity;

import com.mulmi.backend.domain.equipment.entity.EquipmentItem;
import com.mulmi.backend.domain.equipment.entity.EquipmentModel;
import com.mulmi.backend.domain.rental.enums.ApplicationType;
import com.mulmi.backend.domain.rental.enums.RejectReasonType;
import com.mulmi.backend.domain.rental.enums.RentalStatus;
import com.mulmi.backend.domain.semester.entity.Semester;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "rental")
public class Rental extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rental_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    private EquipmentModel equipmentModel;

    // 신청 시점에 배정 (A안). 신청에 성공한 대여는 항상 기기가 있다
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private EquipmentItem equipmentItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private Semester semester;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING)
    @Column(name = "application_type", nullable = false, length = 30)
    private ApplicationType applicationType;

    @Column(name = "purpose", nullable = false, columnDefinition = "TEXT")
    private String purpose;

    // 필드 초기값은 @Builder가 무시하므로 @Builder.Default가 필요하다
    @Builder.Default
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RentalStatus status = RentalStatus.PENDING_APPROVAL;

    // 11-03: 시간 입력 삭제 → 날짜만
    @Column(name = "expected_pickup_date", nullable = false)
    private LocalDate expectedPickupDate;

    // 장기: 신청 때 다음 학기 개강 전날 20:00 / 단기: 실제 수령 때 계산하므로 그때까지 null
    @Column(name = "expected_return_at")
    private LocalDateTime expectedReturnAt;

    // 교수 공통 계정의 실제 교수명 등
    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    // 대여 신청 승인한 조교 (신청 시점에는 null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    // 대여 신청 승인 시각
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    // 반려 사유 (반려됐을 때만 값이 있다)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING)
    @Column(name = "reject_reason_type", length = 30)
    private RejectReasonType rejectReasonType;

    // 반려 상세 사유 (기타일 때만 값이 있다)
    @Column(name = "reject_reason_detail", length = 500)
    private String rejectReasonDetail;

    // 실제로 기기를 가져간 시각 (수령 처리 때 기록)
    @Column(name = "actual_pickup_at")
    private LocalDateTime actualPickupAt;

    // 수령 처리한 근로생·조교
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_checked_by")
    private User pickupCheckedBy;

    // 수령 : 상태·기한·사진 검사와 기기 RENTED 변경, 단기 반납 예정 계산은 서비스가 한다 (A안)
    // expectedReturnAt : 단기는 이번에 계산한 값, 장기는 신청 때 정한 값을 그대로 넘긴다
    public void pickUp(User staff, LocalDateTime now, LocalDateTime expectedReturnAt) {
        this.status = RentalStatus.RENTING;
        this.pickupCheckedBy = staff;
        this.actualPickupAt = now;
        this.expectedReturnAt = expectedReturnAt;
    }

    // 승인 : 상태 검사(PENDING_APPROVAL인지)는 서비스가 한다. 기기는 신청 때 이미 RESERVED라 그대로 둔다
    public void approve(User assistant, LocalDateTime now) {
        this.status = RentalStatus.READY_FOR_PICKUP;
        this.reviewedBy = assistant;
        this.reviewedAt = now;
    }

    // 반려 : 상태·사유 검사와 기기 재고 복귀는 서비스가 한다 (승인과 같은 A안)
    public void reject(User assistant, LocalDateTime now, RejectReasonType reasonType, String reasonDetail) {
        this.status = RentalStatus.REJECTED;
        this.reviewedBy = assistant;
        this.reviewedAt = now;
        this.rejectReasonType = reasonType;
        this.rejectReasonDetail = reasonDetail;
    }
}
