package com.mulmi.backend.domain.blacklist.enums;

public enum BlacklistReasonType {
    NO_SHOW,   // 수령 기간 내 미수령: 해당 학기 대여 제한
    OVERDUE,   // 연체 반납: 다음 학기 대여 제한
    MANUAL     // 관리자가 사유를 확인하고 직접 등록
}
