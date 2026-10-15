package com.mulmi.backend.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "password_reset_verification")
public class PasswordResetVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "verification_code_hash", nullable = false, length = 255)
    private String verificationCodeHash;

    @Column(name = "code_expires_at", nullable = false)
    private LocalDateTime codeExpiresAt;

    @Column(name = "code_sent_at", nullable = false)
    private LocalDateTime codeSentAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "reset_token_hash", unique = true, length = 64)
    private String resetTokenHash;

    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    public void issueCode(
            String verificationCodeHash,
            LocalDateTime codeSentAt,
            LocalDateTime codeExpiresAt
    ) {
        this.verificationCodeHash = verificationCodeHash;
        this.codeSentAt = codeSentAt;
        this.codeExpiresAt = codeExpiresAt;
        this.failedAttempts = 0;
        this.verifiedAt = null;
        this.resetTokenHash = null;
        this.resetTokenExpiresAt = null;
        this.usedAt = null;
    }

    public void increaseFailedAttempts() {
        this.failedAttempts++;
    }

    public void verify(
            LocalDateTime verifiedAt,
            String resetTokenHash,
            LocalDateTime resetTokenExpiresAt
    ) {
        this.verifiedAt = verifiedAt;
        this.resetTokenHash = resetTokenHash;
        this.resetTokenExpiresAt = resetTokenExpiresAt;
    }

    public void consume(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }
}
