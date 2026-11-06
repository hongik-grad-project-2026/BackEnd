package com.mulmi.backend.domain.rental.controller;

import com.mulmi.backend.domain.rental.dto.request.RentalRejectRequestDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalApproveResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalRejectResponseDTO;
import com.mulmi.backend.domain.rental.exception.code.RentalSuccessCode;
import com.mulmi.backend.domain.rental.service.AdminRentalService;
import com.mulmi.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/rentals")
@Tag(name = "대여 관리", description = "조교·근로생용 대여 처리 API")
public class AdminRentalController {

    private final AdminRentalService adminRentalService;

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
