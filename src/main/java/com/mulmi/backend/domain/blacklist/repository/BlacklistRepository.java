package com.mulmi.backend.domain.blacklist.repository;

import com.mulmi.backend.domain.blacklist.entity.Blacklist;
import com.mulmi.backend.domain.blacklist.enums.BlacklistReasonType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface BlacklistRepository extends JpaRepository<Blacklist, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Blacklist> findWithLockById(Long blacklistId);

    boolean existsByUserIdAndSemesterIdAndReleasedAtIsNull(Long userId, Long semesterId);

    boolean existsByRentalIdAndReasonType(Long rentalId, BlacklistReasonType reasonType);

    Page<Blacklist> findAllByUserIdOrderByIdDesc(Long userId, Pageable pageable);
}
