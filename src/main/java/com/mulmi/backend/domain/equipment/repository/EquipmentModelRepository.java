package com.mulmi.backend.domain.equipment.repository;

import com.mulmi.backend.domain.equipment.entity.EquipmentModel;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EquipmentModelRepository extends JpaRepository<EquipmentModel, Long> {

    // 카테고리별 모델 수 집계 결과
    interface CategoryModelCount {
        Long getCategoryId();
        Long getModelCount();
    }

    // 대시보드 63 : 장기 미대여 후보 모델 (별칭과 getter 이름이 같아야 값이 들어감)
    interface ModelIdleRow {
        Long getModelId();
        String getModelName();
        String getCategoryName();
        LocalDate getReceivedDate();
        LocalDateTime getLastReturnedAt();   // 반납 기록이 없으면 null
    }

    // "지금 빌릴 수 있는데 안 빌려간" 모델 (11-20 결정)
    //  폐기되지 않고 대여 가능(AVAILABLE) 상태인 모델 중 AVAILABLE 기기가 1대 이상이고, 지금 RENTED 기기가 없는 모델
    //  → 예약이 몰린 모델(전부 RESERVED)·대여 중지 모델은 빠진다
    //  + 그 모델 대여의 마지막 실제 반납 시각
    @Query("""
            SELECT m.id AS modelId,
                   m.name AS modelName,
                   m.category.name AS categoryName,
                   m.receivedDate AS receivedDate,
                   (SELECT MAX(r.actualReturnAt) FROM Rental r
                     WHERE r.equipmentModel = m) AS lastReturnedAt
            FROM EquipmentModel m
            WHERE m.deletedAt IS NULL
              AND m.rentalStatus = com.mulmi.backend.domain.equipment.enums.EquipmentModelStatus.AVAILABLE
              AND EXISTS (SELECT 1 FROM EquipmentItem i
                           WHERE i.equipmentModel = m
                             AND i.deletedAt IS NULL
                             AND i.status = com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus.AVAILABLE)
              AND NOT EXISTS (SELECT 1 FROM EquipmentItem i
                               WHERE i.equipmentModel = m
                                 AND i.deletedAt IS NULL
                                 AND i.status = com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus.RENTED)
            """)
    List<ModelIdleRow> findIdleCandidates();

    long countByCategoryIdAndDeletedAtIsNull(Long categoryId);

    boolean existsByCategoryIdAndName(Long categoryId, String name);

    // 모델 수정 시 이름 중복 : 자기 자신은 제외 (삭제된 모델은 포함, DB UNIQUE와 같은 기준)
    boolean existsByCategoryIdAndNameAndIdNot(Long categoryId, String name, Long id);

    Optional<EquipmentModel> findByIdAndDeletedAtIsNull(Long id);

    // 모델 행 잠금 : 기기 등록·모델 수정·모델 폐기를 같은 모델 기준으로 한 줄로 세운다
    //  트랜잭션의 첫 조회로 써야 한다 (먼저 일반 조회로 읽어 두면 1차 캐시의 옛 객체를 돌려받는다)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EquipmentModel> findWithLockByIdAndDeletedAtIsNull(Long id);

    // 대여 신청용 공유 잠금 (FOR SHARE) : 신청은 대여 행 INSERT 때 외래 키 검사로 모델 행에 공유 잠금을 건다
    //  기기를 잠근 뒤에 그 잠금이 걸리면 모델 폐기(모델 → 기기)와 순서가 반대라 교착이 난다 → 기기보다 먼저 잡는다
    //  공유 잠금이라 같은 모델에 대한 신청끼리는 서로 막지 않고, 폐기·수정·기기 등록(배타 잠금)과만 줄을 선다
    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<EquipmentModel> findWithShareLockByIdAndDeletedAtIsNull(Long id);

    // 모델 폐기용 : 이미 폐기된 모델도 찾아야 409로 구분한다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EquipmentModel> findWithLockById(Long id);

    // 관리자 모델 목록 : 카테고리·검색어·숨길 카테고리는 null이면 조건 무시
    //  hiddenCategoryName : 학생에게 졸업전시 경품 카테고리를 숨길 때 그 이름
    @Query("""
            SELECT m FROM EquipmentModel m
            WHERE m.deletedAt IS NULL
              AND (:categoryId IS NULL OR m.category.id = :categoryId)
              AND (:keyword IS NULL OR m.name LIKE CONCAT('%', :keyword, '%'))
              AND (:hiddenCategoryName IS NULL OR m.category.name <> :hiddenCategoryName)
            """)
    Page<EquipmentModel> searchModels(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("hiddenCategoryName") String hiddenCategoryName,
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

    // 대여 재고 동기화(Redis) : 폐기되지 않은 모델의 id만 (엔티티 전체를 읽지 않음)
    //  졸업전시 '간식' 카테고리의 '랜덤' 카드는 뺀다 : 자기 기기가 0대라 키가 0이 되면 문지기가 바로 품절로 돌려보낸다
    //  키가 없으면 문지기는 DB로 넘기고, DB에서 경품을 추첨한다 (SnackDraw)
    @Query("""
            SELECT m.id FROM EquipmentModel m
            WHERE m.deletedAt IS NULL
              AND NOT (m.name = '랜덤' AND m.category.name = '간식')
            """)
    List<Long> findAllActiveIds();

    // 졸업전시 랜덤 간식 추첨 후보 : 그 카테고리의 폐기되지 않은 대여 가능 모델 id
    //  엔티티 대신 id만 읽는다 : 뒤에서 뽑힌 모델을 공유 잠금으로 처음 읽어야 1차 캐시의 옛 객체를 받지 않는다
    @Query("""
            SELECT m.id FROM EquipmentModel m
            WHERE m.category.name = :categoryName
              AND m.deletedAt IS NULL
              AND m.rentalStatus = com.mulmi.backend.domain.equipment.enums.EquipmentModelStatus.AVAILABLE
            """)
    List<Long> findRentableIdsByCategoryName(@Param("categoryName") String categoryName);

}
