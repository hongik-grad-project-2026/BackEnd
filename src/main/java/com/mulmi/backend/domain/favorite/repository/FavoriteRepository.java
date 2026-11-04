package com.mulmi.backend.domain.favorite.repository;

import com.mulmi.backend.domain.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserIdAndEquipmentModelId(Long userId, Long equipmentModelId);

    Optional<Favorite> findByUserIdAndEquipmentModelId(Long userId, Long equipmentModelId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM Favorite f
            WHERE f.user.id = :userId
              AND f.equipmentModel.id = :equipmentModelId
            """)
    int deleteByUserIdAndEquipmentModelId(
            @Param("userId") Long userId,
            @Param("equipmentModelId") Long equipmentModelId
    );
}
