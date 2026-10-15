package com.mulmi.backend.domain.user.repository;

import com.mulmi.backend.domain.user.entity.PasswordResetVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetVerificationRepository
        extends JpaRepository<PasswordResetVerification, Long> {

    Optional<PasswordResetVerification> findByUserId(Long userId);

    Optional<PasswordResetVerification> findByResetTokenHash(String resetTokenHash);
}
