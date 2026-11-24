package com.mulmi.backend.domain.semester.repository;

import com.mulmi.backend.domain.semester.entity.Semester;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDate;
import java.util.Optional;

public interface SemesterRepository extends JpaRepository<Semester, Long> {

    // 다음 학기가 아직 없을 때 연체 반납 여러 건이 같은 행을 중복 생성하지 않도록 현재 학기를 잠근다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Semester> findWithLockById(Long semesterId);

    // 지금 학기: 개강일이 오늘 이전(포함)인 학기 중 가장 최근
    Optional<Semester> findFirstByStartDateLessThanEqualOrderByStartDateDesc(LocalDate today);

    // 다음 학기: 개강일이 기준일 이후인 학기 중 가장 이른 것 (반납 마감 계산용)
    Optional<Semester> findFirstByStartDateGreaterThanOrderByStartDateAsc(LocalDate date);

    // 이전 학기: 개강일이 기준일보다 앞선 학기 중 가장 최근 (학기 조회용, 기준일 = 지금 학기 개강일)
    Optional<Semester> findFirstByStartDateLessThanOrderByStartDateDesc(LocalDate date);
}
