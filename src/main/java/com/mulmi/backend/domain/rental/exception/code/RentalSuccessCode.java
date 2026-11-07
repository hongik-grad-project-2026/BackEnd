package com.mulmi.backend.domain.rental.exception.code;

import com.mulmi.backend.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RentalSuccessCode implements BaseSuccessCode {

    // 메시지는 명세 52번 성공 응답
    RENTAL_APPROVED(
            HttpStatus.OK,
            "RENTAL200_1",
            "대여 신청을 승인했습니다."
    ),

    // 메시지는 명세 50번 성공 응답
    RENTALS_FOUND(
            HttpStatus.OK,
            "RENTAL200_3",
            "대여 목록을 조회했습니다."
    ),

    // 메시지는 명세 47번 성공 응답
    MY_RENTALS_FOUND(
            HttpStatus.OK,
            "RENTAL200_6",
            "내 대여 목록을 조회했습니다."
    ),

    // 메시지는 명세 48번 성공 응답
    MY_RENTAL_FOUND(
            HttpStatus.OK,
            "RENTAL200_7",
            "대여 상세를 조회했습니다."
    ),

    // 메시지는 명세 49번 성공 응답
    RENTAL_CANCELED(
            HttpStatus.OK,
            "RENTAL200_8",
            "대여 신청을 취소했습니다."
    ),

    // 메시지는 명세 53번 성공 응답
    RENTAL_REJECTED(
            HttpStatus.OK,
            "RENTAL200_2",
            "대여 신청을 반려했습니다."
    ),

    // 메시지는 명세 54번 성공 응답
    RENTAL_CHECKED_OUT(
            HttpStatus.OK,
            "RENTAL200_4",
            "기자재 대여를 시작했습니다."
    ),

    // 메시지는 명세 57번 성공 응답
    RENTAL_RETURNED(
            HttpStatus.OK,
            "RENTAL200_5",
            "기자재 반납을 완료했습니다."
    ),

    // 메시지는 명세 55번 성공 응답
    RENTAL_IMAGE_UPLOADED(
            HttpStatus.CREATED,
            "RENTAL201_2",
            "사진을 업로드했습니다."
    ),

    // 메시지는 명세 46번 성공 응답
    RENTAL_CREATED(
            HttpStatus.CREATED,
            "RENTAL201_1",
            "대여 신청이 완료되었습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
