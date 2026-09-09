package com.mulmi.backend.domain.user.service;

import com.mulmi.backend.domain.user.dto.response.MyInfoResponseDTO;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.domain.user.enums.UserRole;
import com.mulmi.backend.domain.user.enums.UserStatus;
import com.mulmi.backend.domain.user.exception.UserException;
import com.mulmi.backend.domain.user.exception.code.UserErrorCode;
import com.mulmi.backend.domain.user.repository.UserRepository;
import com.mulmi.backend.global.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserServiceImpl userService;

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
}
