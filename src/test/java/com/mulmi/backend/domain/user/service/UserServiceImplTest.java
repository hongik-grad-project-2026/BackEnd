package com.mulmi.backend.domain.user.service;

import com.mulmi.backend.domain.user.dto.response.MyInfoResponseDTO;
import com.mulmi.backend.domain.user.dto.response.AdminUserPageResponseDTO;
import com.mulmi.backend.domain.user.dto.response.AdminUserDetailResponseDTO;
import com.mulmi.backend.domain.user.dto.request.AdminUpdateUserRequestDTO;
import com.mulmi.backend.domain.user.dto.request.UpdateMyInfoRequestDTO;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
    void getUserReturnsStudentDetail() {
        User user = createUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        AdminUserDetailResponseDTO result = userService.getUser(1L);

        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.loginId()).isEqualTo("C123456");
        assertThat(result.studentId()).isEqualTo("C123456");
        assertThat(result.name()).isEqualTo("홍길동");
        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void getUserDoesNotReturnStaffAccount() {
        User worker = User.builder()
                .id(2L)
                .loginId("worker")
                .password("encoded-password")
                .name("근로생")
                .email("worker@example.com")
                .phoneNumber("01011112222")
                .role(UserRole.WORKER)
                .status(UserStatus.ACTIVE)
                .build();
        given(userRepository.findById(2L)).willReturn(Optional.of(worker));

        assertThatThrownBy(() -> userService.getUser(2L))
                .isInstanceOf(UserException.class)
                .extracting("code")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    void updateUserChangesOnlyProvidedFields() {
        User user = createUser();
        AdminUpdateUserRequestDTO request = new AdminUpdateUserRequestDTO(
                "새 이름",
                "new@example.com",
                null,
                null,
                "소프트웨어학과"
        );
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByEmailAndIdNot("new@example.com", 1L))
                .willReturn(false);

        AdminUserDetailResponseDTO result = userService.updateUser(1L, request);

        assertThat(result.name()).isEqualTo("새 이름");
        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.phoneNumber()).isEqualTo("01012345678");
        assertThat(result.college()).isEqualTo("공과대학");
        assertThat(result.department()).isEqualTo("소프트웨어학과");
    }

    @Test
    void updateUserThrowsWhenEmailIsAlreadyInUse() {
        User user = createUser();
        AdminUpdateUserRequestDTO request = new AdminUpdateUserRequestDTO(
                null,
                "duplicate@example.com",
                null,
                null,
                null
        );
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByEmailAndIdNot("duplicate@example.com", 1L))
                .willReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, request))
                .isInstanceOf(UserException.class)
                .extracting("code")
                .isEqualTo(UserErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void updateUserThrowsWhenRequestHasNoValues() {
        User user = createUser();
        AdminUpdateUserRequestDTO request = new AdminUpdateUserRequestDTO(
                null,
                " ",
                null,
                "",
                null
        );
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateUser(1L, request))
                .isInstanceOf(UserException.class)
                .extracting("code")
                .isEqualTo(UserErrorCode.EMPTY_UPDATE_REQUEST);
    }

    @Test
    void getUsersReturnsFilteredStudentPage() {
        User user = createUser();
        PageRequest pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.DESC, "id")
        );
        given(userRepository.findUsers(
                UserRole.STUDENT,
                "홍",
                UserStatus.ACTIVE,
                "공과대학",
                "컴퓨터공학과",
                pageable
        )).willReturn(new PageImpl<>(List.of(user), pageable, 1));

        AdminUserPageResponseDTO result = userService.getUsers(
                " 홍 ",
                UserStatus.ACTIVE,
                " 공과대학 ",
                " 컴퓨터공학과 ",
                0,
                20
        );

        assertThat(result.users()).hasSize(1);
        assertThat(result.users().get(0).userId()).isEqualTo(1L);
        assertThat(result.users().get(0).studentId()).isEqualTo("C123456");
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void getUsersTreatsBlankFiltersAsMissing() {
        PageRequest pageable = PageRequest.of(
                1,
                10,
                Sort.by(Sort.Direction.DESC, "id")
        );
        given(userRepository.findUsers(
                UserRole.STUDENT,
                null,
                null,
                null,
                null,
                pageable
        )).willReturn(new PageImpl<>(List.of(), pageable, 0));

        userService.getUsers(" ", null, "", "   ", 1, 10);

        verify(userRepository).findUsers(
                UserRole.STUDENT,
                null,
                null,
                null,
                null,
                pageable
        );
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

    @Test
    void withdrawChangesStatusAndRecordsDeletedTime() {
        User user = createUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userService.withdraw(1L);

        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(user.getDeletedAt()).isNotNull();
    }

    @Test
    void withdrawThrowsWhenUserDoesNotExist() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.withdraw(999L))
                .isInstanceOf(UserException.class)
                .extracting("code")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    void withdrawnUserCannotAccessMyInfo() {
        User user = createUser();
        user.withdraw();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.getMyInfo(1L))
                .isInstanceOf(UserException.class)
                .extracting("code")
                .isEqualTo(UserErrorCode.INACTIVE_USER);
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
