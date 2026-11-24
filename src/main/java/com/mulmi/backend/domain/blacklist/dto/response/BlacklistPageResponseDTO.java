package com.mulmi.backend.domain.blacklist.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record BlacklistPageResponseDTO(
        List<BlacklistResponseDTO> blacklists,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static BlacklistPageResponseDTO from(Page<BlacklistResponseDTO> page) {
        return new BlacklistPageResponseDTO(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
