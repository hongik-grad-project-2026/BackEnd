package com.mulmi.backend.domain.blacklist.repository;

import com.mulmi.backend.domain.blacklist.entity.Blacklist;
import com.mulmi.backend.domain.blacklist.enums.BlacklistReasonType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistRepository extends JpaRepository<Blacklist, Long> {

    boolean existsByUserIdAndSemesterIdAndReleasedAtIsNull(Long userId, Long semesterId);

    boolean existsByRentalIdAndReasonType(Long rentalId, BlacklistReasonType reasonType);
}
