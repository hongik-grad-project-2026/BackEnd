package com.mulmi.backend.domain.rental.repository;

import com.mulmi.backend.domain.rental.entity.Rental;
import com.mulmi.backend.domain.rental.enums.RentalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.Optional;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    // 1인 1대: 같은 카테고리에서 진행 중(statuses)인 대여가 있는지
    // Rental.user.id, Rental.equipmentModel.category.id 를 따라간다
    boolean existsByUserIdAndEquipmentModelCategoryIdAndStatusIn(
            Long userId, Long categoryId, Collection<RentalStatus> statuses);

    // 상태를 바꾸는 관리자 처리(승인 등): 대여 행을 잠가 같은 대여에 대한 동시 처리를 한 줄로 세운다
    //  SELECT ... FROM rental WHERE rental_id = ? FOR UPDATE
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Rental> findWithLockById(Long rentalId);
}
