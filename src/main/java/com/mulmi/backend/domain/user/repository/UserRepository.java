package com.mulmi.backend.domain.user.repository;

import com.mulmi.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    boolean existsByStudentId(String studentId);

    Optional<User> findByLoginId(String loginId);
}