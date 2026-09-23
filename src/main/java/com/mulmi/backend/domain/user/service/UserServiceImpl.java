package com.mulmi.backend.domain.user.service;

import com.mulmi.backend.domain.user.converter.UserConverter;
import com.mulmi.backend.domain.user.dto.request.LoginRequestDTO;
import com.mulmi.backend.domain.user.dto.request.SignupRequestDTO;
import com.mulmi.backend.domain.user.dto.request.UpdateMyInfoRequestDTO;
import com.mulmi.backend.domain.user.dto.response.LoginResponseDTO;
import com.mulmi.backend.domain.user.dto.response.MyInfoResponseDTO;
import com.mulmi.backend.domain.user.dto.response.SignupResponseDTO;
import com.mulmi.backend.domain.user.dto.response.AdminUserPageResponseDTO;
import com.mulmi.backend.domain.user.dto.response.AdminUserSummaryResponseDTO;
import com.mulmi.backend.domain.user.dto.response.AdminUserDetailResponseDTO;
import com.mulmi.backend.domain.user.dto.request.AdminUpdateUserRequestDTO;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.domain.user.enums.UserRole;
import com.mulmi.backend.domain.user.enums.UserStatus;
import com.mulmi.backend.domain.user.exception.UserException;
import com.mulmi.backend.domain.user.exception.code.UserErrorCode;
import com.mulmi.backend.domain.user.repository.UserRepository;

import com.mulmi.backend.global.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    //회원가입
    @Override
    @Transactional
    public SignupResponseDTO signup(SignupRequestDTO dto) {
        validateDuplicateUser(dto); //중복검사 메서드 호출

        String encodedPassword = passwordEncoder.encode(dto.password());
        User user = UserConverter.toUser(dto, encodedPassword);
        User savedUser = userRepository.save(user);

        return UserConverter.toSignupResponseDTO(savedUser);
    }

    //로그인
    @Override
    public LoginResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByLoginId(dto.loginId()) //사용자가 없으면 예외
                .orElseThrow(() -> new UserException(UserErrorCode.INVALID_CREDENTIALS));
        //비밀번호 비교
        if (!passwordEncoder.matches(dto.password(), user.getPassword())){
            throw new UserException(UserErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserException(UserErrorCode.INACTIVE_USER);
        }
        //비밀번호가 일치하면 jwt 생서
        String accessToken = jwtUtil.createAccessToken(
                user.getId(),
                user.getLoginId(),
                user.getRole()
        );
        //로그인 응답 변환
        return new LoginResponseDTO(
                user.getId(),
                user.getLoginId(),
                user.getName(),
                user.getRole(),
                user.getStatus(),
                accessToken
        );
    }

    @Override
    public MyInfoResponseDTO getMyInfo(Long userId) {
        User user = findActiveUser(userId);

        return UserConverter.toMyInfoResponseDTO(user);
    }

    @Override
    @Transactional
    public MyInfoResponseDTO updateMyInfo(Long userId, UpdateMyInfoRequestDTO dto) {
        User user = findActiveUser(userId);

        if (userRepository.existsByEmailAndIdNot(dto.email(), userId)) {
            throw new UserException(UserErrorCode.DUPLICATE_EMAIL);
        }

        user.updateContactInfo(dto.email(), dto.phoneNumber());

        return UserConverter.toMyInfoResponseDTO(user);
    }

    // 회원탈퇴
    @Override
    @Transactional
    public void withdraw(Long userId) {
        User user = findActiveUser(userId);

        user.withdraw();
    }

    // 회원 목록 조회
    @Override
    public AdminUserPageResponseDTO getUsers(
            String keyword,
            UserStatus status,
            String college,
            String department,
            int page,
            int size
    ) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        );
        Page<User> users = userRepository.findUsers(
                UserRole.STUDENT,
                normalize(keyword),
                status,
                normalize(college),
                normalize(department),
                pageable
        );
        Page<AdminUserSummaryResponseDTO> responsePage = users.map(
                UserConverter::toAdminUserSummaryResponseDTO
        );

        return AdminUserPageResponseDTO.from(responsePage);
    }

    // 특정 학생 정보 조회
    @Override
    public AdminUserDetailResponseDTO getUser(Long userId) {
        return UserConverter.toAdminUserDetailResponseDTO(findStudent(userId));
    }

    // 학생 정보 수정
    @Override
    @Transactional
    public AdminUserDetailResponseDTO updateUser(
            Long userId,
            AdminUpdateUserRequestDTO dto
    ) {
        User user = findStudent(userId);
        String name = normalize(dto.name());
        String email = normalize(dto.email());
        String phoneNumber = normalize(dto.phoneNumber());
        String college = normalize(dto.college());
        String department = normalize(dto.department());

        if (name == null
                && email == null
                && phoneNumber == null
                && college == null
                && department == null) {
            throw new UserException(UserErrorCode.EMPTY_UPDATE_REQUEST);
        }

        if (email != null && userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new UserException(UserErrorCode.DUPLICATE_EMAIL);
        }

        user.updateByAdmin(name, email, phoneNumber, college, department);
        userRepository.flush();
        return UserConverter.toAdminUserDetailResponseDTO(user);
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserException(UserErrorCode.INACTIVE_USER);
        }
        return user;
    }

    private User findStudent(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        if (user.getRole() != UserRole.STUDENT) {
            throw new UserException(UserErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    //중복 검사 메서드
    private void validateDuplicateUser(SignupRequestDTO dto) {
        if (userRepository.existsByLoginId(dto.studentId())) {
            throw new UserException(UserErrorCode.DUPLICATE_LOGIN_ID);
        }

        if (userRepository.existsByStudentId(dto.studentId())) {
            throw new UserException(UserErrorCode.DUPLICATE_STUDENT_ID);
        }

        if (userRepository.existsByEmail(dto.email())) {
            throw new UserException(UserErrorCode.DUPLICATE_EMAIL);
        }

    }
}
