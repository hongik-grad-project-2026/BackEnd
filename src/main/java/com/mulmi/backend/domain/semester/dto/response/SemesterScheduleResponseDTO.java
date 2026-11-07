package com.mulmi.backend.domain.semester.dto.response;

import com.mulmi.backend.domain.semester.entity.Semester;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 학기 조회(명세 60) : 이전·현재·다음 학기 (DB에 없으면 null)
// 배열 대신 이름을 붙여, 프론트가 현재 학기를 result.current로 바로 꺼내게 한다
public record SemesterScheduleResponseDTO(
        SemesterInfo previous,
        SemesterInfo current,
        SemesterInfo next
) {
    // 날짜 계산은 Semester 엔티티에만 두고 여기서는 결과를 옮겨 담기만 한다 (프론트도 계산하지 않고 보여주기만)
    public record SemesterInfo(
            Long semesterId,
            int year,
            int term,
            LocalDate startDate,
            LocalDate preApplyStartDate,
            LocalDate preApplyEndDate,       // 수령 기간 시작 전날
            LocalDate pickupStartDate,
            LocalDateTime pickupDeadline,    // 수령 기간 마지막 날 20:00 (지나면 노쇼)
            LocalDate onDemandStartDate,
            LocalDateTime returnDeadline     // 다음 학기 개강 전날 20:00 (상시 신청도 이때까지)
    ) {
        // nextSemesterStart : 이 학기 다음 학기의 개강일 (DB에 없으면 3/1·9/1 기본값)
        public static SemesterInfo of(Semester semester, LocalDate nextSemesterStart) {
            LocalDate pickupStartDate = semester.pickupStart().toLocalDate();
            return new SemesterInfo(
                    semester.getId(),
                    semester.academicYear(),
                    semester.term(),
                    semester.getStartDate(),
                    semester.preApplyStart().toLocalDate(),
                    pickupStartDate.minusDays(1),
                    pickupStartDate,
                    semester.pickupDeadline(),
                    semester.onDemandStart().toLocalDate(),
                    Semester.returnDueAt(nextSemesterStart)
            );
        }
    }
}
