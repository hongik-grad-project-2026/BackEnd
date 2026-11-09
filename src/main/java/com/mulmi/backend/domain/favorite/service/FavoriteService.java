package com.mulmi.backend.domain.favorite.service;

import com.mulmi.backend.domain.favorite.dto.response.FavoritePageResponseDTO;

public interface FavoriteService {

    void addFavorite(Long userId, Long equipmentModelId);

    void removeFavorite(Long userId, Long equipmentModelId);

    FavoritePageResponseDTO getMyFavorites(Long userId, int page, int size);
}
