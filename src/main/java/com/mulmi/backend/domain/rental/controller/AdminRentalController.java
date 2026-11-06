package com.mulmi.backend.domain.rental.controller;

import com.mulmi.backend.domain.rental.dto.request.RentalRejectRequestDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalApproveResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalRejectResponseDTO;
import com.mulmi.backend.domain.rental.exception.code.RentalSuccessCode;
import com.mulmi.backend.domain.rental.service.AdminRentalService;
import com.mulmi.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.mulmi.backend.domain.rental.dto.response.AdminRentalPageResponseDTO;
import com.mulmi.backend.domain.rental.enums.RentalStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/rentals")
@Tag(name = "대여 관리", description = "조교·근로생용 대여 처리 API")
public class AdminRentalController {

    private final AdminRentalService adminRentalService;

    // 관리자 대여 목록 (명세 50번) : 승인·현황·기록 화면이 status만 바꿔 가며 같이 쓴다
    //  status는 쉼표로 여러 개 (예 : READY_FOR_PICKUP,RENTING,OVERDUE), 없으면 전체
    @Operation(summary = "관리자 대여 목록")
    @GetMapping
    @PreAuthorize("hasAnyRole('ASSISTANT', 'WORKER')")
    public ApiResponse<AdminRentalPageResponseDTO> getAdminRentals(
            @RequestParam(required = false) List<RentalStatus> status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.onSuccess(
                RentalSuccessCode.RENTALS_FOUND,
                adminRentalService.getAdminRentals(status, keyword, page, size)
        );
    }

    // 대여 승인 (명세 52번) : 근로생은 승인할 수 없다
    @Operation(summary = "대여 승인")
    @PatchMapping("/{rental_id}/approve")
    @PreAuthorize("hasRole('ASSISTANT')")
    public ApiResponse<RentalApproveResponseDTO> approveRental(
            @PathVariable("rental_id") Long rentalId,
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.onSuccess(
                RentalSuccessCode.RENTAL_APPROVED,
                adminRentalService.approveRental(rentalId, userId)
        );
    }

    // 대여 반려 (명세 53번)
    @Operation(summary = "대여 반려")
    @PatchMapping("/{rental_id}/reject")
    @PreAuthorize("hasRole('ASSISTANT')")
    public ApiResponse<RentalRejectResponseDTO> rejectRental(
            @PathVariable("rental_id") Long rentalId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid RentalRejectRequestDTO dto
    ) {
        return ApiResponse.onSuccess(
                RentalSuccessCode.RENTAL_REJECTED,
                adminRentalService.rejectRental(rentalId, userId, dto)
        );
    }
}
