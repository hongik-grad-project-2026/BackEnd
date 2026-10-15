package com.mulmi.backend.domain.user.service;

import com.mulmi.backend.domain.user.dto.request.PasswordResetCodeConfirmRequestDTO;
import com.mulmi.backend.domain.user.dto.request.PasswordResetCodeRequestDTO;
import com.mulmi.backend.domain.user.dto.request.PasswordResetRequestDTO;
import com.mulmi.backend.domain.user.dto.response.PasswordResetResponseDTO;
import com.mulmi.backend.domain.user.dto.response.PasswordResetTokenResponseDTO;
import com.mulmi.backend.domain.user.entity.PasswordResetVerification;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.domain.user.enums.UserStatus;
import com.mulmi.backend.domain.user.exception.UserException;
import com.mulmi.backend.domain.user.exception.code.UserErrorCode;
import com.mulmi.backend.domain.user.repository.PasswordResetVerificationRepository;
import com.mulmi.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int CODE_BOUND = 1_000_000;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long CODE_EXPIRES_IN_SECONDS = 300;
    private static final long RESET_TOKEN_EXPIRES_IN_SECONDS = 600;
    private static final long RESEND_COOLDOWN_SECONDS = 60;

    private final UserRepository userRepository;
    private final PasswordResetVerificationRepository verificationRepository;
    private final PasswordResetEmailSender emailSender;
    private final PasswordResetAttemptService attemptService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void sendVerificationCode(PasswordResetCodeRequestDTO dto) {
        Optional<User> optionalUser = findMatchingActiveUser(
                dto.studentId(),
                dto.name(),
                dto.email()
        );

        // 계정 존재 여부가 외부에 노출되지 않도록 일치하지 않아도 같은 성공 응답을 반환한다.
        if (optionalUser.isEmpty()) {
            return;
        }

        User user = optionalUser.get();
        LocalDateTime now = LocalDateTime.now();
        PasswordResetVerification verification = verificationRepository
                .findByUserId(user.getId())
                .orElse(null);

        if (verification != null
                && verification.getCodeSentAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
            throw new UserException(UserErrorCode.PASSWORD_RESET_RESEND_TOO_SOON);
        }

        String verificationCode = "%06d".formatted(secureRandom.nextInt(CODE_BOUND));
        String verificationCodeHash = passwordEncoder.encode(verificationCode);

        if (verification == null) {
            verification = PasswordResetVerification.builder()
                    .user(user)
                    .verificationCodeHash(verificationCodeHash)
                    .codeSentAt(now)
                    .codeExpiresAt(now.plusSeconds(CODE_EXPIRES_IN_SECONDS))
                    .failedAttempts(0)
                    .build();
        } else {
            verification.issueCode(
                    verificationCodeHash,
                    now,
                    now.plusSeconds(CODE_EXPIRES_IN_SECONDS)
            );
        }

        verificationRepository.save(verification);
        emailSender.sendVerificationCode(user.getEmail(), verificationCode);
    }

    @Override
    @Transactional
    public PasswordResetTokenResponseDTO confirmVerificationCode(
            PasswordResetCodeConfirmRequestDTO dto
    ) {
        User user = findMatchingActiveUser(dto.studentId(), dto.name(), dto.email())
                .orElseThrow(() -> new UserException(
                        UserErrorCode.PASSWORD_RESET_VERIFICATION_NOT_FOUND
                ));
        PasswordResetVerification verification = verificationRepository
                .findByUserId(user.getId())
                .orElseThrow(() -> new UserException(
                        UserErrorCode.PASSWORD_RESET_VERIFICATION_NOT_FOUND
                ));
        LocalDateTime now = LocalDateTime.now();

        if (verification.getCodeExpiresAt().isBefore(now)) {
            throw new UserException(UserErrorCode.PASSWORD_RESET_CODE_EXPIRED);
        }
        if (verification.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new UserException(UserErrorCode.PASSWORD_RESET_TOO_MANY_ATTEMPTS);
        }
        if (!passwordEncoder.matches(
                dto.verificationCode(),
                verification.getVerificationCodeHash()
        )) {
            int failedAttempts = attemptService.recordFailure(verification.getId());
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                throw new UserException(UserErrorCode.PASSWORD_RESET_TOO_MANY_ATTEMPTS);
            }
            throw new UserException(UserErrorCode.PASSWORD_RESET_CODE_INVALID);
        }

        String resetToken = generateResetToken();
        verification.verify(
                now,
                sha256(resetToken),
                now.plusSeconds(RESET_TOKEN_EXPIRES_IN_SECONDS)
        );

        return new PasswordResetTokenResponseDTO(
                resetToken,
                RESET_TOKEN_EXPIRES_IN_SECONDS
        );
    }

    @Override
    @Transactional
    public PasswordResetResponseDTO resetPassword(PasswordResetRequestDTO dto) {
        if (!dto.newPassword().equals(dto.newPasswordConfirm())) {
            throw new UserException(UserErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        PasswordResetVerification verification = verificationRepository
                .findByResetTokenHash(sha256(dto.resetToken()))
                .orElseThrow(() -> new UserException(
                        UserErrorCode.PASSWORD_RESET_TOKEN_INVALID
                ));
        LocalDateTime now = LocalDateTime.now();

        if (verification.getVerifiedAt() == null || verification.getUsedAt() != null) {
            throw new UserException(UserErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }
        if (verification.getResetTokenExpiresAt() == null
                || verification.getResetTokenExpiresAt().isBefore(now)) {
            throw new UserException(UserErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }

        User user = verification.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserException(UserErrorCode.INACTIVE_USER);
        }

        user.changePassword(passwordEncoder.encode(dto.newPassword()));
        verification.consume(now);

        return new PasswordResetResponseDTO(
                user.getStudentId(),
                user.getName(),
                user.getEmail(),
                user.getCollege(),
                user.getDepartment()
        );
    }

    private Optional<User> findMatchingActiveUser(
            String studentId,
            String name,
            String email
    ) {
        return userRepository.findByStudentIdAndNameAndEmailAndStatus(
                studentId,
                name,
                email,
                UserStatus.ACTIVE
        );
    }

    private String generateResetToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}
