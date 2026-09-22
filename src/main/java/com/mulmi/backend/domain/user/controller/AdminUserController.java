package com.mulmi.backend.domain.user.controller;

import com.mulmi.backend.domain.user.dto.response.AdminUserPageResponseDTO;
import com.mulmi.backend.domain.user.dto.response.AdminUserDetailResponseDTO;
import com.mulmi.backend.domain.user.dto.request.AdminUpdateUserRequestDTO;
import com.mulmi.backend.domain.user.enums.UserStatus;
import com.mulmi.backend.domain.user.exception.code.UserSuccessCode;
import com.mulmi.backend.domain.user.service.UserService;
import com.mulmi.backend.global.apiPayload.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    // 회원 목록 조회
    @GetMapping
    @PreAuthorize("hasAnyRole('ASSISTANT', 'WORKER')")
    public ApiResponse<AdminUserPageResponseDTO> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String college,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.onSuccess(
                UserSuccessCode.USERS_FOUND,
                userService.getUsers(keyword, status, college, department, page, size)
        );
    }

    // 특정 학생 정보 조회
    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ASSISTANT', 'WORKER')")
    public ApiResponse<AdminUserDetailResponseDTO> getUser(
            @PathVariable Long userId
    ) {
        return ApiResponse.onSuccess(
                UserSuccessCode.USER_DETAIL_FOUND,
                userService.getUser(userId)
        );
    }

    // 학생 정보 수정
    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('ASSISTANT')")
    public ApiResponse<AdminUserDetailResponseDTO> updateUser(
            @PathVariable Long userId,
            @RequestBody @Valid AdminUpdateUserRequestDTO dto
    ) {
        return ApiResponse.onSuccess(
                UserSuccessCode.USER_UPDATED_BY_ADMIN,
                userService.updateUser(userId, dto)
        );
    }
}
