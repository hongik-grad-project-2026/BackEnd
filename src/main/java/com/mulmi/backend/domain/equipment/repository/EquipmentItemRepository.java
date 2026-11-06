package com.mulmi.backend.domain.equipment.repository;

import com.mulmi.backend.domain.equipment.entity.EquipmentItem;
import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    Optional<EquipmentItem> findByIdAndDeletedAtIsNull(Long id);

    // 기기 수정: 대여 신청(기기 잠금 후 RESERVED)과 동시에 실행돼도 최신 상태를 보고 판단하도록 잠그며 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EquipmentItem> findWithLockByIdAndDeletedAtIsNull(Long id);

    // 모델에 살아 있는 기기가 있는지 (모델 카테고리·접두사 변경 제한)
    boolean existsByEquipmentModelIdAndDeletedAtIsNull(Long modelId);

    // 모델에 특정 상태의 살아 있는 기기가 있는지 (모델 폐기 제한)
    boolean existsByEquipmentModelIdAndDeletedAtIsNullAndStatusIn(
            Long modelId, Collection<EquipmentItemStatus> statuses);

    // 모델 폐기 시 함께 폐기할 기기
    List<EquipmentItem> findAllByEquipmentModelIdAndDeletedAtIsNull(Long modelId);

    // 일괄 등록할 라벨 중 이미 쓰인 게 하나라도 있는지 (폐기된 기기 포함 — 라벨은 재사용하지 않음)
    boolean existsByLabelCodeIn(Collection<String> labelCodes);

    // 대여 신청: 모델의 대여 가능 기기 중 id가 가장 작은 1대를 잠그며 가져온다
    // → ORDER BY id LIMIT 1 FOR UPDATE
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EquipmentItem> findFirstByEquipmentModelIdAndStatusAndDeletedAtIsNullOrderByIdAsc(
            Long modelId, EquipmentItemStatus status);
}
