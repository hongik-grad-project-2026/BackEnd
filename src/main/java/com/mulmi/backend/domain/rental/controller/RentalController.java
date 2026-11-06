package com.mulmi.backend.domain.rental.controller;

import com.mulmi.backend.domain.rental.dto.request.RentalCreateRequestDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalCreateResponseDTO;
import com.mulmi.backend.domain.rental.exception.code.RentalSuccessCode;
import com.mulmi.backend.domain.rental.service.RentalService;
import com.mulmi.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
}
