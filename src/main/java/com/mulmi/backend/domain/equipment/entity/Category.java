package com.mulmi.backend.domain.equipment.entity;

import com.mulmi.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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

    public void updateCategoryName(String name) {
        this.name = name;
    }

}
