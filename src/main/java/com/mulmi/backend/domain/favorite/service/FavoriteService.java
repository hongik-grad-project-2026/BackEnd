package com.mulmi.backend.domain.favorite.service;

public interface FavoriteService {

    void addFavorite(Long userId, Long equipmentModelId);

    void removeFavorite(Long userId, Long equipmentModelId);
}
