package com.mulmi.backend.domain.equipment.entity;

import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "equipment_item")
public class EquipmentItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private EquipmentModel equipmentModel;

    @Column(name="label_number")
    private Integer labelNumber;

    @Column(name="label_code", length = 30, unique = true)
    private String labelCode;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING)
    @Column(name="status", length = 30, nullable = false)
    private EquipmentItemStatus status;

    @Column(name = "memo", nullable = true, columnDefinition = "TEXT")
    private String memo;

    public void changeStatus(EquipmentItemStatus status) {
        this.status = status;
    }

    // 번호와 코드는 항상 함께 바뀐다
    public void changeLabel(int labelNumber, String labelCode) {
        this.labelNumber = labelNumber;
        this.labelCode = labelCode;
    }

    public void changeMemo(String memo) {
        this.memo = memo;
    }

    // 폐기 = 소프트 삭제 (과거 대여 기록이 이 기기를 가리키므로 행은 남긴다)
    public void dispose() {
        markDeleted();
    }
}
