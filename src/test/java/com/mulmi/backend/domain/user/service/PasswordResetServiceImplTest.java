package com.mulmi.backend.domain.user.service;

import com.mulmi.backend.domain.user.dto.request.PasswordResetCodeConfirmRequestDTO;
import com.mulmi.backend.domain.user.dto.request.PasswordResetCodeRequestDTO;
import com.mulmi.backend.domain.user.dto.request.PasswordResetRequestDTO;
import com.mulmi.backend.domain.user.dto.response.PasswordResetResponseDTO;
import com.mulmi.backend.domain.user.dto.response.PasswordResetTokenResponseDTO;
import com.mulmi.backend.domain.user.entity.PasswordResetVerification;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.domain.user.enums.UserRole;
import com.mulmi.backend.domain.user.enums.UserStatus;
import com.mulmi.backend.domain.user.exception.UserException;
import com.mulmi.backend.domain.user.exception.code.UserErrorCode;
import com.mulmi.backend.domain.user.repository.PasswordResetVerificationRepository;
import com.mulmi.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetVerificationRepository verificationRepository;

    @Mock
    private PasswordResetEmailSender emailSender;

    @Mock
    private PasswordResetAttemptService attemptService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private PasswordResetServiceImpl passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetServiceImpl(
                userRepository,
                verificationRepository,
                emailSender,
                attemptService,
                passwordEncoder
        );
    }

    @Test
    void 계정정보가_일치하지_않으면_존재여부를_숨기고_메일을_보내지_않는다() {
        PasswordResetCodeRequestDTO request = new PasswordResetCodeRequestDTO(
                "C000000",
                "홍길동",
                "student@hongik.ac.kr"
        );
        when(userRepository.findByStudentIdAndNameAndEmailAndStatus(
                request.studentId(),
                request.name(),
                request.email(),
                UserStatus.ACTIVE
        )).thenReturn(Optional.empty());

        passwordResetService.sendVerificationCode(request);

        verify(emailSender, never()).sendVerificationCode(any(), any());
        verify(verificationRepository, never()).save(any());
    }

    @Test
    void 올바른_인증번호를_확인하면_10분짜리_일회용_재설정_토큰을_발급한다() {
        String code = "123456";
        User user = createUser("old-password");
        PasswordResetVerification verification = createVerification(
                user,
                passwordEncoder.encode(code),
                LocalDateTime.now().plusMinutes(5)
        );
        PasswordResetCodeConfirmRequestDTO request = new PasswordResetCodeConfirmRequestDTO(
                user.getStudentId(),
                user.getName(),
                user.getEmail(),
                code
        );
        mockMatchedUser(request.studentId(), request.name(), request.email(), user);
        when(verificationRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(verification));

        PasswordResetTokenResponseDTO response =
                passwordResetService.confirmVerificationCode(request);

        assertThat(response.resetToken()).isNotBlank();
        assertThat(response.expiresInSeconds()).isEqualTo(600);
        assertThat(verification.getVerifiedAt()).isNotNull();
        assertThat(verification.getResetTokenHash()).isEqualTo(sha256(response.resetToken()));
    }

    @Test
    void 인증번호가_만료되면_재설정_토큰을_발급하지_않는다() {
        User user = createUser("old-password");
        PasswordResetVerification verification = createVerification(
                user,
                passwordEncoder.encode("123456"),
                LocalDateTime.now().minusSeconds(1)
        );
        PasswordResetCodeConfirmRequestDTO request = new PasswordResetCodeConfirmRequestDTO(
                user.getStudentId(),
                user.getName(),
                user.getEmail(),
                "123456"
        );
        mockMatchedUser(request.studentId(), request.name(), request.email(), user);
        when(verificationRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> passwordResetService.confirmVerificationCode(request))
                .isInstanceOf(UserException.class)
                .satisfies(exception -> assertThat(((UserException) exception).getCode())
                        .isEqualTo(UserErrorCode.PASSWORD_RESET_CODE_EXPIRED));
    }

    @Test
    void 유효한_토큰으로_비밀번호를_암호화해_변경하고_토큰을_소비한다() {
        String resetToken = "test-reset-token";
        User user = createUser(passwordEncoder.encode("old-password"));
        PasswordResetVerification verification = createVerification(
                user,
                passwordEncoder.encode("123456"),
                LocalDateTime.now().plusMinutes(5)
        );
        verification.verify(
                LocalDateTime.now(),
                sha256(resetToken),
                LocalDateTime.now().plusMinutes(10)
        );
        when(verificationRepository.findByResetTokenHash(sha256(resetToken)))
                .thenReturn(Optional.of(verification));
        PasswordResetRequestDTO request = new PasswordResetRequestDTO(
                resetToken,
                "new-password",
                "new-password"
        );

        PasswordResetResponseDTO response = passwordResetService.resetPassword(request);

        assertThat(passwordEncoder.matches("new-password", user.getPassword())).isTrue();
        assertThat(verification.getUsedAt()).isNotNull();
        assertThat(response.studentId()).isEqualTo(user.getStudentId());
    }

    @Test
    void 이미_사용한_재설정_토큰은_다시_사용할_수_없다() {
        String resetToken = "used-reset-token";
        User user = createUser(passwordEncoder.encode("old-password"));
        PasswordResetVerification verification = createVerification(
                user,
                passwordEncoder.encode("123456"),
                LocalDateTime.now().plusMinutes(5)
        );
        verification.verify(
                LocalDateTime.now(),
                sha256(resetToken),
                LocalDateTime.now().plusMinutes(10)
        );
        verification.consume(LocalDateTime.now());
        when(verificationRepository.findByResetTokenHash(sha256(resetToken)))
                .thenReturn(Optional.of(verification));
        PasswordResetRequestDTO request = new PasswordResetRequestDTO(
                resetToken,
                "new-password",
                "new-password"
        );

        assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                .isInstanceOf(UserException.class)
                .satisfies(exception -> assertThat(((UserException) exception).getCode())
                        .isEqualTo(UserErrorCode.PASSWORD_RESET_TOKEN_INVALID));
    }

    private void mockMatchedUser(
            String studentId,
            String name,
            String email,
            User user
    ) {
        when(userRepository.findByStudentIdAndNameAndEmailAndStatus(
                studentId,
                name,
                email,
                UserStatus.ACTIVE
        )).thenReturn(Optional.of(user));
    }

    private User createUser(String password) {
        return User.builder()
                .id(1L)
                .loginId("C000000")
                .studentId("C000000")
                .name("홍길동")
                .email("student@hongik.ac.kr")
                .phoneNumber("010-0000-0000")
                .college("공과대학")
                .department("컴퓨터공학과")
                .password(password)
                .role(UserRole.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private PasswordResetVerification createVerification(
            User user,
            String codeHash,
            LocalDateTime expiresAt
    ) {
        return PasswordResetVerification.builder()
                .id(1L)
                .user(user)
                .verificationCodeHash(codeHash)
                .codeSentAt(LocalDateTime.now().minusMinutes(1))
                .codeExpiresAt(expiresAt)
                .failedAttempts(0)
                .build();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
