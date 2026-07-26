package com.mulmi.backend.domain.user.service;

import com.mulmi.backend.domain.user.converter.UserConverter;
import com.mulmi.backend.domain.user.dto.request.LoginRequestDTO;
import com.mulmi.backend.domain.user.dto.request.SignupRequestDTO;
import com.mulmi.backend.domain.user.dto.response.LoginResponseDTO;
import com.mulmi.backend.domain.user.dto.response.SignupResponseDTO;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.domain.user.exception.UserException;
import com.mulmi.backend.domain.user.exception.code.UserErrorCode;
import com.mulmi.backend.domain.user.repository.UserRepository;

import com.mulmi.backend.global.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
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
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        //비밀번호 비교
        if (!passwordEncoder.matches(dto.password(), user.getPassword())){
            throw new UserException(UserErrorCode.INVALID_PASSWORD);
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

        if (userRepository.existsByPhoneNumber(dto.phoneNumber())) {
            throw new UserException(UserErrorCode.DUPLICATE_PHONE_NUMBER);
        }
    }
}
