package com.mulmi.backend.domain.rental.repository;

import com.mulmi.backend.domain.rental.entity.Rental;
import com.mulmi.backend.domain.rental.enums.RentalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    // 1인 1대: 같은 카테고리에서 진행 중(statuses)인 대여가 있는지
    // Rental.user.id, Rental.equipmentModel.category.id 를 따라간다
    boolean existsByUserIdAndEquipmentModelCategoryIdAndStatusIn(
            Long userId, Long categoryId, Collection<RentalStatus> statuses);

    // 관리자 대여 목록 (명세 50) : 연관 엔티티를 fetch join으로 한 번에 가져온다 (N+1 방지)
    // 연체는 저장하지 않고 계산한다 : RENTING이면서 반납 예정 시각이 지난 것 (11-07 결정)
    //  - plainStatuses : RENTING·OVERDUE를 뺀 나머지 요청 상태 (비면 서비스가 OVERDUE를 넣는다 — DB에 저장되지 않는 값이라 아무것도 걸리지 않음)
    //  - wantRenting / wantOverdue : RENTING 행을 연체 여부로 나눠서 고른다
    //  ※ 상태 조건은 searchMyRentals(47)와 같다 — 연체 규칙이 바뀌면 두 쿼리를 함께 고칠 것
    @Query(value = """
            SELECT r FROM Rental r
            JOIN FETCH r.user u
            JOIN FETCH r.equipmentModel m
            JOIN FETCH m.category c
            JOIN FETCH r.equipmentItem i
            WHERE (:allStatuses = true
                   OR r.status IN :plainStatuses
                   OR (r.status = com.mulmi.backend.domain.rental.enums.RentalStatus.RENTING
                       AND ((:wantRenting = true AND (r.expectedReturnAt IS NULL OR r.expectedReturnAt >= :now))
                            OR (:wantOverdue = true AND r.expectedReturnAt < :now))))
              AND (:keyword IS NULL
                   OR u.name LIKE CONCAT('%', :keyword, '%')
                   OR u.studentId LIKE CONCAT('%', :keyword, '%')
                   OR m.name LIKE CONCAT('%', :keyword, '%')
                   OR i.labelCode LIKE CONCAT('%', :keyword, '%'))
            ORDER BY r.id DESC
            """,
            countQuery = """
            SELECT COUNT(r) FROM Rental r
            JOIN r.user u
            JOIN r.equipmentModel m
            JOIN r.equipmentItem i
            WHERE (:allStatuses = true
                   OR r.status IN :plainStatuses
                   OR (r.status = com.mulmi.backend.domain.rental.enums.RentalStatus.RENTING
                       AND ((:wantRenting = true AND (r.expectedReturnAt IS NULL OR r.expectedReturnAt >= :now))
                            OR (:wantOverdue = true AND r.expectedReturnAt < :now))))
              AND (:keyword IS NULL
                   OR u.name LIKE CONCAT('%', :keyword, '%')
                   OR u.studentId LIKE CONCAT('%', :keyword, '%')
                   OR m.name LIKE CONCAT('%', :keyword, '%')
                   OR i.labelCode LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<Rental> searchAdminRentals(
            @Param("allStatuses") boolean allStatuses,
            @Param("plainStatuses") Collection<RentalStatus> plainStatuses,
            @Param("wantRenting") boolean wantRenting,
            @Param("wantOverdue") boolean wantOverdue,
            @Param("now") LocalDateTime now,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // 상태별 대여 수 (탭 옆 숫자)
    interface StatusCount {
        RentalStatus getStatus();
        Long getCount();
    }

    @Query("SELECT r.status AS status, COUNT(r) AS count FROM Rental r GROUP BY r.status")
    List<StatusCount> countByStatus();

    // 연체 수 : RENTING 중 반납 예정 시각이 지난 것
    @Query("""
            SELECT COUNT(r) FROM Rental r
            WHERE r.status = com.mulmi.backend.domain.rental.enums.RentalStatus.RENTING
              AND r.expectedReturnAt < :now
            """)
    long countOverdue(@Param("now") LocalDateTime now);

    // 내 대여 목록 (명세 47) : 50과 같은 상태 조건 + 로그인한 학생의 대여만 (userId는 항상 걸린다)
    //  keyword는 학생 화면 기준 : 모델명·카테고리·라벨 (신청자 이름·학번은 본인이라 뺌)
    //  ※ 상태(RENTING·OVERDUE) 조건은 searchAdminRentals와 같다 — 연체 규칙이 바뀌면 두 쿼리를 함께 고칠 것
    @Query(value = """
            SELECT r FROM Rental r
            JOIN FETCH r.equipmentModel m
            JOIN FETCH m.category c
            JOIN FETCH r.equipmentItem i
            WHERE r.user.id = :userId
              AND (:allStatuses = true
                   OR r.status IN :plainStatuses
                   OR (r.status = com.mulmi.backend.domain.rental.enums.RentalStatus.RENTING
                       AND ((:wantRenting = true AND (r.expectedReturnAt IS NULL OR r.expectedReturnAt >= :now))
                            OR (:wantOverdue = true AND r.expectedReturnAt < :now))))
              AND (:keyword IS NULL
                   OR m.name LIKE CONCAT('%', :keyword, '%')
                   OR c.name LIKE CONCAT('%', :keyword, '%')
                   OR i.labelCode LIKE CONCAT('%', :keyword, '%'))
            ORDER BY r.id DESC
            """,
            countQuery = """
            SELECT COUNT(r) FROM Rental r
            JOIN r.equipmentModel m
            JOIN m.category c
            JOIN r.equipmentItem i
            WHERE r.user.id = :userId
              AND (:allStatuses = true
                   OR r.status IN :plainStatuses
                   OR (r.status = com.mulmi.backend.domain.rental.enums.RentalStatus.RENTING
                       AND ((:wantRenting = true AND (r.expectedReturnAt IS NULL OR r.expectedReturnAt >= :now))
                            OR (:wantOverdue = true AND r.expectedReturnAt < :now))))
              AND (:keyword IS NULL
                   OR m.name LIKE CONCAT('%', :keyword, '%')
                   OR c.name LIKE CONCAT('%', :keyword, '%')
                   OR i.labelCode LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<Rental> searchMyRentals(
            @Param("userId") Long userId,
            @Param("allStatuses") boolean allStatuses,
            @Param("plainStatuses") Collection<RentalStatus> plainStatuses,
            @Param("wantRenting") boolean wantRenting,
            @Param("wantOverdue") boolean wantOverdue,
            @Param("now") LocalDateTime now,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // 내 대여 상세 (명세 48) : 대여 id와 로그인한 학생을 함께 건다 → 남의 대여는 결과가 비어 404 (존재 여부도 숨김)
    @Query("""
            SELECT r FROM Rental r
            JOIN FETCH r.equipmentModel m
            JOIN FETCH m.category c
            JOIN FETCH r.equipmentItem i
            WHERE r.id = :rentalId
              AND r.user.id = :userId
            """)
    Optional<Rental> findMyRental(@Param("rentalId") Long rentalId, @Param("userId") Long userId);

    // 내 대여의 상태별 수 (47 탭 옆 숫자)
    @Query("SELECT r.status AS status, COUNT(r) AS count FROM Rental r WHERE r.user.id = :userId GROUP BY r.status")
    List<StatusCount> countByStatusAndUserId(@Param("userId") Long userId);

    // 내 대여 중 연체 수
    @Query("""
            SELECT COUNT(r) FROM Rental r
            WHERE r.user.id = :userId
              AND r.status = com.mulmi.backend.domain.rental.enums.RentalStatus.RENTING
              AND r.expectedReturnAt < :now
            """)
    long countOverdueByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    // 상태를 바꾸는 관리자 처리(승인 등): 대여 행을 잠가 같은 대여에 대한 동시 처리를 한 줄로 세운다
    //  SELECT ... FROM rental WHERE rental_id = ? FOR UPDATE
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Rental> findWithLockById(Long rentalId);
}
