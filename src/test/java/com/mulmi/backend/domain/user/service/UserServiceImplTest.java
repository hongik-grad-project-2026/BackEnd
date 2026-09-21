package com.mulmi.backend.domain.user.service;

import com.mulmi.backend.domain.user.dto.response.MyInfoResponseDTO;
import com.mulmi.backend.domain.user.dto.request.UpdateMyInfoRequestDTO;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.domain.user.enums.UserRole;
import com.mulmi.backend.domain.user.enums.UserStatus;
import com.mulmi.backend.domain.user.exception.UserException;
import com.mulmi.backend.domain.user.exception.code.UserErrorCode;
import com.mulmi.backend.domain.user.repository.UserRepository;
import com.mulmi.backend.global.jwt.JwtUtil;
import com.mulmi.backend.global.jwt.TokenBlacklist;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklist tokenBlacklist;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void logoutRevokesAccessTokenUntilItExpires() {
        Claims claims = mock(Claims.class);
        Date expiration = new Date(System.currentTimeMillis() + 60_000);
        given(jwtUtil.extractAllClaims("access-token")).willReturn(claims);
        given(jwtUtil.extractExpiration(claims)).willReturn(expiration);

        userService.logout("access-token");

        verify(tokenBlacklist).revoke("access-token", expiration.toInstant());
    }

    @Test
    void getMyInfoReturnsCurrentUser() {
        User user = User.builder()
                .id(1L)
                .loginId("C123456")
                .studentId("C123456")
                .password("encoded-password")
                .name("홍길동")
                .email("hong@example.com")
                .phoneNumber("01012345678")
                .college("공과대학")
                .department("컴퓨터공학과")
                .role(UserRole.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        MyInfoResponseDTO result = userService.getMyInfo(1L);

        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.loginId()).isEqualTo("C123456");
        assertThat(result.name()).isEqualTo("홍길동");
        assertThat(result.studentId()).isEqualTo("C123456");
        assertThat(result.email()).isEqualTo("hong@example.com");
        assertThat(result.phoneNumber()).isEqualTo("01012345678");
        assertThat(result.college()).isEqualTo("공과대학");
        assertThat(result.department()).isEqualTo("컴퓨터공학과");
        assertThat(result.profileImageUrl()).isNull();
        assertThat(result.role()).isEqualTo(UserRole.STUDENT);
        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void getMyInfoThrowsWhenUserDoesNotExist() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyInfo(999L))
                .isInstanceOf(UserException.class)
                .extracting("code")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    void updateMyInfoChangesEmailAndPhoneNumber() {
        User user = createUser();
        UpdateMyInfoRequestDTO request = new UpdateMyInfoRequestDTO(
                "new@example.com",
                "01098765432"
        );
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByEmailAndIdNot("new@example.com", 1L))
                .willReturn(false);

        MyInfoResponseDTO result = userService.updateMyInfo(1L, request);

        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.phoneNumber()).isEqualTo("01098765432");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getPhoneNumber()).isEqualTo("01098765432");
    }

    @Test
    void updateMyInfoThrowsWhenEmailIsAlreadyInUse() {
        User user = createUser();
        UpdateMyInfoRequestDTO request = new UpdateMyInfoRequestDTO(
                "duplicate@example.com",
                "01098765432"
        );
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByEmailAndIdNot("duplicate@example.com", 1L))
                .willReturn(true);

        assertThatThrownBy(() -> userService.updateMyInfo(1L, request))
                .isInstanceOf(UserException.class)
                .extracting("code")
                .isEqualTo(UserErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void updateMyInfoThrowsWhenUserDoesNotExist() {
        UpdateMyInfoRequestDTO request = new UpdateMyInfoRequestDTO(
                "new@example.com",
                "01098765432"
        );
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateMyInfo(999L, request))
                .isInstanceOf(UserException.class)
                .extracting("code")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    private User createUser() {
        return User.builder()
                .id(1L)
                .loginId("C123456")
                .studentId("C123456")
                .password("encoded-password")
                .name("홍길동")
                .email("hong@example.com")
                .phoneNumber("01012345678")
                .college("공과대학")
                .department("컴퓨터공학과")
                .role(UserRole.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
