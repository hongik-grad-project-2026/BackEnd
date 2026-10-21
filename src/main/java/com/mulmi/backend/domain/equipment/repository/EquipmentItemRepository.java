package com.mulmi.backend.domain.equipment.repository;

import com.mulmi.backend.domain.equipment.entity.EquipmentItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentItemRepository extends JpaRepository<EquipmentItem, Long> {
}
