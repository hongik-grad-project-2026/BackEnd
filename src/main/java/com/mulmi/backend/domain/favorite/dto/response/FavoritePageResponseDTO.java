package com.mulmi.backend.domain.favorite.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record FavoritePageResponseDTO(
        List<FavoriteModelResponseDTO> favorites,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static FavoritePageResponseDTO from(Page<FavoriteModelResponseDTO> page) {
        return new FavoritePageResponseDTO(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
