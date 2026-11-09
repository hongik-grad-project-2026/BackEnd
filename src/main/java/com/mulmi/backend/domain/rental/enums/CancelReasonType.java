package com.mulmi.backend.domain.rental.enums;

// 학생 신청 취소 사유 (명세 49, 11-07 결정 : 화면의 3개 중 하나를 고른다)
// ERD의 cancel_reason은 직접 입력(TEXT)이었으나 화면에 맞춰 고정 값으로 바꿨다 — 화면 문구는 프론트가 붙인다
public enum CancelReasonType {
    NO_LONGER_NEEDED,       // 필요 없어짐
    WRONG_APPLICATION_INFO, // 신청 정보 잘못 입력함
    APPROVAL_DELAYED        // 승인이 늦음
}
