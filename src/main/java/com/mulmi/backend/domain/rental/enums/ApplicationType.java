package com.mulmi.backend.domain.rental.enums;

// 신청 방식. 클라이언트가 보내지 않고 서버가 신청 시각으로 정한다
public enum ApplicationType {
    REGULAR,    // 사전신청 기간에 접수
    ON_DEMAND   // 상시 기간에 접수
}
