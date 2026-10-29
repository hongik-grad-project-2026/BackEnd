package com.mulmi.backend.domain.equipment.repository;

import com.mulmi.backend.domain.equipment.entity.EquipmentModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface EquipmentModelRepository extends JpaRepository<EquipmentModel, Long> {

    // 카테고리별 모델 수 집계 결과
    interface CategoryModelCount {
        Long getCategoryId();
        Long getModelCount();
    }

    long countByCategoryIdAndDeletedAtIsNull(Long categoryId);

    boolean existsByCategoryIdAndName(Long categoryId, String name);

    Optional<EquipmentModel> findByIdAndDeletedAtIsNull(Long id);

    // 관리자 모델 목록 : 카테고리·검색어는 null이면 조건 무시
    @Query("""
            SELECT m FROM EquipmentModel m
            WHERE m.deletedAt IS NULL
              AND (:categoryId IS NULL OR m.category.id = :categoryId)
              AND (:keyword IS NULL OR m.name LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<EquipmentModel> searchModels(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // 카테고리 탭 옆 숫자 : 삭제되지 않은 모델 수 (검색어·선택 탭과 무관)
    @Query("""
            SELECT m.category.id AS categoryId,
                   COUNT(m) AS modelCount
            FROM EquipmentModel m
            WHERE m.deletedAt IS NULL
            GROUP BY m.category.id
            """)
    List<CategoryModelCount> countModelsByCategory();

}
