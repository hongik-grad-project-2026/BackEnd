package com.mulmi.backend.domain.rental.controller;

import com.mulmi.backend.domain.rental.dto.request.RentalCancelRequestDTO;
import com.mulmi.backend.domain.rental.dto.request.RentalCreateRequestDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalCancelResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalCreateResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalDetailResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalPageResponseDTO;
import com.mulmi.backend.domain.rental.enums.RentalStatus;
import com.mulmi.backend.domain.rental.exception.code.RentalSuccessCode;
import com.mulmi.backend.domain.rental.service.RentalService;
import com.mulmi.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rentals")
@Tag(name = "대여", description = "학생용 대여 API")
public class RentalController {

    private final RentalService rentalService;

    // 대여 신청 (명세 46번) : 신청자는 본문이 아니라 토큰에서 꺼낸다
    @Operation(summary = "대여 신청")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<RentalCreateResponseDTO> createRental(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid RentalCreateRequestDTO dto
    ) {
        return ApiResponse.onSuccess(
                RentalSuccessCode.RENTAL_CREATED,
                rentalService.createRental(userId, dto)
        );
    }

    // 내 대여 목록 (명세 47번) : 조회 대상은 토큰의 사용자로 고정 (다른 사람 대여는 볼 수 없다)
    //  status는 50과 같이 쉼표로 여러 개 (대여중 탭 = RENTING,OVERDUE), 없으면 전체
    @Operation(summary = "내 대여 목록")
    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<RentalPageResponseDTO> getMyRentals(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) List<RentalStatus> status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.onSuccess(
                RentalSuccessCode.MY_RENTALS_FOUND,
                rentalService.getMyRentals(userId, status, keyword, page, size)
        );
    }

    // 내 대여 상세 (명세 48번) : 남의 대여 번호를 넣어도 404 (존재 여부를 드러내지 않는다)
    @Operation(summary = "내 대여 상세")
    @GetMapping("/me/{rental_id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<RentalDetailResponseDTO> getMyRental(
            @AuthenticationPrincipal Long userId,
            @PathVariable("rental_id") Long rentalId
    ) {
        return ApiResponse.onSuccess(
                RentalSuccessCode.MY_RENTAL_FOUND,
                rentalService.getMyRental(userId, rentalId)
        );
    }

    // 신청 취소 (명세 49번) : 승인 대기·수령 대기만, 취소 사유는 화면의 3개 중 하나
    @Operation(summary = "대여 신청 취소")
    @PatchMapping("/me/{rental_id}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<RentalCancelResponseDTO> cancelRental(
            @AuthenticationPrincipal Long userId,
            @PathVariable("rental_id") Long rentalId,
            @RequestBody @Valid RentalCancelRequestDTO dto
    ) {
        return ApiResponse.onSuccess(
                RentalSuccessCode.RENTAL_CANCELED,
                rentalService.cancelRental(userId, rentalId, dto)
        );
    }
}
