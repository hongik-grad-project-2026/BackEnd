package com.mulmi.backend.domain.equipment.repository;

import com.mulmi.backend.domain.equipment.entity.EquipmentModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EquipmentModelRepository extends JpaRepository<EquipmentModel, Long> {

    long countByCategoryIdAndDeletedAtIsNull(Long categoryId);

    boolean existsByCategoryIdAndName(Long categoryId, String name);

    Optional<EquipmentModel> findByIdAndDeletedAtIsNull(Long id);

}
