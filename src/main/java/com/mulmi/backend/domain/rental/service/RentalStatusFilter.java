package com.mulmi.backend.domain.rental.service;

import com.mulmi.backend.domain.rental.enums.RentalStatus;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

// 목록 상태 필터(47·50)를 쿼리 파라미터 4개로 바꾼 것
// RENTING·OVERDUE는 "DB의 RENTING 행을 연체 여부로 나눈 것"이라 IN 조건과 따로 넘긴다
//  - allStatuses   : 필터 없음(전체)
//  - plainStatuses : RENTING·OVERDUE를 뺀 나머지 요청 상태
//  - wantRenting / wantOverdue : RENTING 행 중 연체 아닌 것 / 연체인 것
record RentalStatusFilter(
        boolean allStatuses,
        Set<RentalStatus> plainStatuses,
        boolean wantRenting,
        boolean wantOverdue
) {
    static RentalStatusFilter of(List<RentalStatus> statuses) {
        boolean allStatuses = statuses == null || statuses.isEmpty();
        boolean wantRenting = !allStatuses && statuses.contains(RentalStatus.RENTING);
        boolean wantOverdue = !allStatuses && statuses.contains(RentalStatus.OVERDUE);

        Set<RentalStatus> plainStatuses = allStatuses ? EnumSet.noneOf(RentalStatus.class) : EnumSet.copyOf(statuses);
        plainStatuses.removeAll(EnumSet.of(RentalStatus.RENTING, RentalStatus.OVERDUE));
        if (plainStatuses.isEmpty()) {
            // 빈 IN ()은 SQL 오류가 날 수 있어, DB에 저장되지 않는 OVERDUE를 넣어 "아무것도 안 걸리는" 조건으로 만든다
            plainStatuses.add(RentalStatus.OVERDUE);
        }
        return new RentalStatusFilter(allStatuses, plainStatuses, wantRenting, wantOverdue);
    }
}
