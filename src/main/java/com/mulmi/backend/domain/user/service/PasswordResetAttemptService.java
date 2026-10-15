package com.mulmi.backend.domain.user.service;

import com.mulmi.backend.domain.user.entity.PasswordResetVerification;
import com.mulmi.backend.domain.user.repository.PasswordResetVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetAttemptService {

    private final PasswordResetVerificationRepository verificationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordFailure(Long verificationId) {
        PasswordResetVerification verification = verificationRepository
                .findById(verificationId)
                .orElseThrow();
        verification.increaseFailedAttempts();
        return verification.getFailedAttempts();
    }
}
