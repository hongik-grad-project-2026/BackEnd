package com.mulmi.backend.domain.equipment.repository;

import com.mulmi.backend.domain.equipment.entity.LabelPrefix;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface LabelPrefixRepository extends JpaRepository<LabelPrefix, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LabelPrefix> findWithLockById(Long labelPrefixId);
}
