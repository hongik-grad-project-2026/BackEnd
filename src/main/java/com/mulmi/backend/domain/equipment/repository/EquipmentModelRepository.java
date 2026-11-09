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

    // 모델 수정 시 이름 중복 : 자기 자신은 제외 (삭제된 모델은 포함, DB UNIQUE와 같은 기준)
    boolean existsByCategoryIdAndNameAndIdNot(Long categoryId, String name, Long id);

    Optional<EquipmentModel> findByIdAndDeletedAtIsNull(Long id);

    // 잠금 이후 재확인용 : 엔티티가 아니라 값만 받아야 1차 캐시의 옛 객체가 아닌 최신 커밋 값을 읽는다
    // 폐기된 모델이면 빈 값, 접두사가 없으면 0
    @Query("""
            SELECT COALESCE(p.id, 0) FROM EquipmentModel m
            LEFT JOIN m.labelPrefix p
            WHERE m.id = :modelId
              AND m.deletedAt IS NULL
            """)
    Optional<Long> findActivePrefixId(@Param("modelId") Long modelId);

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
