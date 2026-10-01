package com.mulmi.backend.domain.equipment.repository;

import com.mulmi.backend.domain.equipment.entity.EquipmentModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentModelRepository extends JpaRepository<EquipmentModel, Long> {

    long countByCategoryIdAndDeletedAtIsNull(Long categoryId);

}
