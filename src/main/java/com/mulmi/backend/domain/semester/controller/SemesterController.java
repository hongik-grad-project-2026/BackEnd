package com.mulmi.backend.domain.semester.controller;

import com.mulmi.backend.domain.semester.dto.response.SemesterScheduleResponseDTO;
import com.mulmi.backend.domain.semester.exception.code.SemesterSuccessCode;
import com.mulmi.backend.domain.semester.service.SemesterService;
import com.mulmi.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/semesters")
@Tag(name = "학기", description = "학기 일정 API")
public class SemesterController {

    private final SemesterService semesterService;

    // 학기 일정 조회 (명세 60번)
    // 명세는 /api/admin/semesters(조교)지만, 학생 화면도 사전신청·수령 기간을 보여줘야 해서 로그인한 누구나 볼 수 있게 했다
    //  @PreAuthorize가 없으면 SecurityConfig의 anyRequest().authenticated()만 적용된다 (역할 무관, 토큰만 있으면 통과)
    @Operation(summary = "학기 일정 조회 (이전·현재·다음 학기)")
    @GetMapping
    public ApiResponse<SemesterScheduleResponseDTO> getSemesterSchedule() {
        return ApiResponse.onSuccess(
                SemesterSuccessCode.SEMESTER_SCHEDULE_FOUND,
                semesterService.getSemesterSchedule()
        );
    }
}
