package com.mulmi.backend.domain.user.repository;

import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.domain.user.enums.UserRole;
import com.mulmi.backend.domain.user.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByStudentId(String studentId);

    Optional<User> findByLoginId(String loginId);

    @Query("""
            SELECT u
            FROM User u
            WHERE u.role = :role
              AND (:status IS NULL OR u.status = :status)
              AND (:college IS NULL OR u.college = :college)
              AND (:department IS NULL OR u.department = :department)
              AND (
                    :keyword IS NULL
                    OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(u.studentId) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            """)
    Page<User> findUsers(
            @Param("role") UserRole role,
            @Param("keyword") String keyword,
            @Param("status") UserStatus status,
            @Param("college") String college,
            @Param("department") String department,
            Pageable pageable
    );
}
