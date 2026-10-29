package com.mulmi.backend.domain.equipment.repository;

import com.mulmi.backend.domain.equipment.entity.EquipmentItem;
import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EquipmentItemRepository extends JpaRepository<EquipmentItem, Long> {

    // 모델별 재고 집계 결과 (별칭과 getter 이름이 같아야 값이 들어감)
    interface ModelStockCount {
        Long getModelId();
        Long getTotalCount();
        Long getAvailableCount();
    }

    // 모델별 전체 기기 수(폐기 = 삭제 제외)와 대여 가능 기기 수
    @Query("""
            SELECT i.equipmentModel.id AS modelId,
                   COUNT(i) AS totalCount,
                   SUM(CASE WHEN i.status = com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus.AVAILABLE
                            THEN 1 ELSE 0 END) AS availableCount
            FROM EquipmentItem i
            WHERE i.equipmentModel.id IN :modelIds
              AND i.deletedAt IS NULL
            GROUP BY i.equipmentModel.id
            """)
    List<ModelStockCount> countStockByModelIds(@Param("modelIds") List<Long> modelIds);

    // 모델별 기기 목록 : 상태·라벨 검색어는 null이면 조건 무시
    @Query("""
            SELECT i FROM EquipmentItem i
            WHERE i.equipmentModel.id = :modelId
              AND i.deletedAt IS NULL
              AND (:status IS NULL OR i.status = :status)
              AND (:keyword IS NULL OR i.labelCode LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<EquipmentItem> searchItems(
            @Param("modelId") Long modelId,
            @Param("status") EquipmentItemStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    interface StatusCount {
        EquipmentItemStatus getStatus();
        Long getCount();
    }

    @Query("""
          SELECT i.status AS status,
                 COUNT(i) AS count
          FROM EquipmentItem i
          WHERE i.equipmentModel.id = :modelId
              AND i.deletedAt IS NULL
          GROUP BY i.status
          """)
    List<StatusCount> countByStatus(@Param("modelId") Long modelId);
}
