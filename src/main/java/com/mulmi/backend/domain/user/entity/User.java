package com.mulmi.backend.domain.user.entity;

import com.mulmi.backend.domain.user.enums.UserRole;
import com.mulmi.backend.domain.user.enums.UserStatus;
import com.mulmi.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //학생은 학번, 근로생/조교는 별도의 id로 로그인.
    @Column(name = "login_id", nullable = false, unique = true, length = 30)
    private String loginId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // 암호화된 비밀번호 저장
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 30)
    private String name;

    //단과대학
    @Column(nullable = false, length = 50)
    private String college;

    // 학과
    @Column(nullable = false, length = 50)
    private String dept;

    // 학번 (학생은 학번, 근로생/조교는 null 가능)
    @Column(name = "student_id", unique = true, length = 20)
    private String studentId;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;
}