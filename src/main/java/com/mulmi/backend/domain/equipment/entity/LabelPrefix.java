package com.mulmi.backend.domain.equipment.entity;

import com.mulmi.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "label_prefix")
public class LabelPrefix extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prefix_code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "prefix_name", nullable = false, length = 50)
    private String name;

    @Column(name = "last_number", nullable = false)
    private int lastNumber;

    public int issueNextNumber() {
        this.lastNumber++;
        return this.lastNumber;
    }

}
