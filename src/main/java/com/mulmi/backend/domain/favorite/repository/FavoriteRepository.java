package com.mulmi.backend.domain.favorite.repository;

import com.mulmi.backend.domain.favorite.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndEquipmentModelId(Long userId, Long equipmentModelId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO favorites (user_id, equipment_model_id, created_at, updated_at)
            VALUES (:userId, :equipmentModelId, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE id = id
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("userId") Long userId,
            @Param("equipmentModelId") Long equipmentModelId
    );

    @Query(
            value = """
                    SELECT f FROM Favorite f
                    JOIN FETCH f.equipmentModel m
                    JOIN FETCH m.category
                    WHERE f.user.id = :userId
                      AND m.deletedAt IS NULL
                    """,
            countQuery = """
                    SELECT COUNT(f) FROM Favorite f
                    WHERE f.user.id = :userId
                      AND f.equipmentModel.deletedAt IS NULL
                    """
    )
    Page<Favorite> findFavoritePageByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
            SELECT f.equipmentModel.id FROM Favorite f
            WHERE f.user.id = :userId
              AND f.equipmentModel.id IN :equipmentModelIds
            """)
    List<Long> findFavoriteModelIds(
            @Param("userId") Long userId,
            @Param("equipmentModelIds") List<Long> equipmentModelIds
    );

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
