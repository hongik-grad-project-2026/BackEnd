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

    // 기기 폐기: 대여 신청과 한 줄로 서도록 잠그며 조회 (이미 폐기된 기기도 찾아야 409로 구분하므로 DeletedAtIsNull 없음)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EquipmentItem> findWithLockById(Long id);

    // 모델 폐기: 살아 있는 기기를 전부 잠그며 조회 → 이 목록으로 예약·대여 중인지 검사하고 함께 폐기
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<EquipmentItem> findAllWithLockByEquipmentModelIdAndDeletedAtIsNull(Long modelId);

    // 접두사 없는 번호 중 가장 큰 번호 (자동 발급용). 폐기된 기기·폐기된 모델도 포함 — 폐기 번호를 다시 주지 않기 위해
    //  카테고리가 아니라 코드 형식으로 거른다 : 중복 검사(label_code UNIQUE)가 코드 기준이므로 범위를 똑같이 맞춘다
    //  접두사 있는 코드는 모두 '-'를 포함(M-007)하므로 '-' 없는 코드가 곧 접두사 없는 번호 공간이다
    //  (접두사 없는 카테고리가 여럿이어도, 기기가 폐기된 채 모델이 다른 카테고리로 옮겨 가도 빠지지 않는다)
    @Query("""
            SELECT MAX(i.labelNumber)
            FROM EquipmentItem i
            WHERE i.labelCode NOT LIKE '%-%'
            """)
    Optional<Integer> findMaxUnprefixedLabelNumber();

    // 일괄 등록할 라벨 중 이미 쓰인 게 하나라도 있는지 (폐기된 기기 포함 — 라벨은 재사용하지 않음)
    boolean existsByLabelCodeIn(Collection<String> labelCodes);

    // 대여 신청: 모델의 대여 가능 기기 중 라벨 번호가 가장 큰(가장 최근에 들인) 1대를 잠그며 가져온다
    // 라벨은 구입 순서대로 붙이므로 번호가 클수록 최신 연식이다. 라벨이 없는 기기는 맨 뒤, 그 안에서는 id 역순
    // → ORDER BY label_number DESC, id DESC LIMIT 1 FOR UPDATE
    // id도 DESC여야 인덱스(model_id, status, label_number)를 끝에서 거꾸로 읽어 1행만 잠근다
    // (id ASC면 순서가 안 맞아 대여 가능 기기를 전부 읽고 정렬 → 전부 잠김)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EquipmentItem> findFirstByEquipmentModelIdAndStatusAndDeletedAtIsNullOrderByLabelNumberDescIdDesc(
            Long modelId, EquipmentItemStatus status);
}
