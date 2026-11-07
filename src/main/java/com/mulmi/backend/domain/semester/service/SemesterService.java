package com.mulmi.backend.domain.semester.service;

import com.mulmi.backend.domain.semester.dto.response.SemesterScheduleResponseDTO;

public interface SemesterService {

    // 학기 일정 조회 (명세 60번) : 이전·현재·다음 학기
    SemesterScheduleResponseDTO getSemesterSchedule();
}
