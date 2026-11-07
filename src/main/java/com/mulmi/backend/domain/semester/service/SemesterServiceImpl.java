package com.mulmi.backend.domain.semester.service;

import com.mulmi.backend.domain.semester.dto.response.SemesterScheduleResponseDTO;
import com.mulmi.backend.domain.semester.dto.response.SemesterScheduleResponseDTO.SemesterInfo;
import com.mulmi.backend.domain.semester.entity.Semester;
import com.mulmi.backend.domain.semester.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SemesterServiceImpl implements SemesterService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private final SemesterRepository semesterRepository;

    // 학기는 DB에 직접 넣는다 (개강일 3/1·9/1 고정이라 등록·수정 API 없음). 없는 자리는 null
    @Override
    public SemesterScheduleResponseDTO getSemesterSchedule() {
        LocalDate today = LocalDate.now(KOREA);

        // 지금 학기 : 46 신청과 같은 기준 (개강일 ≤ 오늘 중 가장 최근)
        Semester current = semesterRepository
                .findFirstByStartDateLessThanEqualOrderByStartDateDesc(today)
                .orElse(null);
        Semester previous = current == null ? null : semesterRepository
                .findFirstByStartDateLessThanOrderByStartDateDesc(current.getStartDate())
                .orElse(null);
        Semester next = semesterRepository
                .findFirstByStartDateGreaterThanOrderByStartDateAsc(today)
                .orElse(null);

        // 반납 마감에 쓰는 "그 학기 다음 개강일" : 바로 뒤 학기가 있으면 그 개강일, 없으면 3/1·9/1 기본값
        return new SemesterScheduleResponseDTO(
                toInfo(previous, current),
                toInfo(current, next),
                toInfo(next, null)
        );
    }

    private SemesterInfo toInfo(Semester semester, Semester following) {
        if (semester == null) {
            return null;
        }
        LocalDate nextStart = following != null ? following.getStartDate() : semester.defaultNextStartDate();
        return SemesterInfo.of(semester, nextStart);
    }
}
