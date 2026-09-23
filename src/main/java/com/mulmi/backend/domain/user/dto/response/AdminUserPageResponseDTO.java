package com.mulmi.backend.domain.user.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminUserPageResponseDTO(
        List<AdminUserSummaryResponseDTO> users,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static AdminUserPageResponseDTO from(Page<AdminUserSummaryResponseDTO> page) {
        return new AdminUserPageResponseDTO(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
