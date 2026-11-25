package com.mulmi.backend.domain.blacklist.repository;

import com.mulmi.backend.domain.blacklist.entity.Blacklist;
import com.mulmi.backend.domain.blacklist.enums.BlacklistReasonType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface BlacklistRepository extends JpaRepository<Blacklist, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Blacklist> findWithLockById(Long blacklistId);

    boolean existsByUserIdAndSemesterIdAndReleasedAtIsNull(Long userId, Long semesterId);

    boolean existsByUserIdAndReleasedAtIsNull(Long userId);

    @Query("""
            SELECT DISTINCT b.user.id
            FROM Blacklist b
            WHERE b.user.id IN :userIds
              AND b.releasedAt IS NULL
            """)
    Set<Long> findActiveUserIds(@Param("userIds") Collection<Long> userIds);

    boolean existsByRentalIdAndReasonType(Long rentalId, BlacklistReasonType reasonType);

    Page<Blacklist> findAllByUserIdOrderByIdDesc(Long userId, Pageable pageable);
}
