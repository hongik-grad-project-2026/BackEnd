package com.mulmi.backend.domain.favorite.repository;

import com.mulmi.backend.domain.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserIdAndEquipmentModelId(Long userId, Long equipmentModelId);

    Optional<Favorite> findByUserIdAndEquipmentModelId(Long userId, Long equipmentModelId);
}
