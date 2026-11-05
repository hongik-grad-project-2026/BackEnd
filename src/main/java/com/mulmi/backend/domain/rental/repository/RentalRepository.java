package com.mulmi.backend.domain.rental.repository;

import com.mulmi.backend.domain.rental.entity.Rental;
import com.mulmi.backend.domain.rental.enums.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    // 1인 1대: 같은 카테고리에서 진행 중(statuses)인 대여가 있는지
    // Rental.user.id, Rental.equipmentModel.category.id 를 따라간다
    boolean existsByUserIdAndEquipmentModelCategoryIdAndStatusIn(
            Long userId, Long categoryId, Collection<RentalStatus> statuses);
}
