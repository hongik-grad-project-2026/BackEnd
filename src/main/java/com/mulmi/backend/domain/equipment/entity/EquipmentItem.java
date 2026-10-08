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

    @Column(name="label_number", nullable = false)
    private Integer labelNumber;

    @Column(name="label_code", length = 30, nullable = false, unique = true)
    private String labelCode;

    @Enumerated(EnumType.STRING)
    @Column(name="status", length = 30, nullable = false)
    private EquipmentItemStatus status;

    @Column(name = "memo", nullable = true, columnDefinition = "TEXT")
    private String memo;
}
