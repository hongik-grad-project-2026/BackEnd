package com.mulmi.backend.domain.favorite.service;

import com.mulmi.backend.domain.equipment.entity.EquipmentModel;
import com.mulmi.backend.domain.equipment.exception.EquipmentModelException;
import com.mulmi.backend.domain.equipment.exception.code.EquipmentModelErrorCode;
import com.mulmi.backend.domain.equipment.repository.EquipmentModelRepository;
import com.mulmi.backend.domain.favorite.entity.Favorite;
import com.mulmi.backend.domain.favorite.repository.FavoriteRepository;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.domain.user.enums.UserStatus;
import com.mulmi.backend.domain.user.exception.UserException;
import com.mulmi.backend.domain.user.exception.code.UserErrorCode;
import com.mulmi.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final EquipmentModelRepository equipmentModelRepository;

    @Override
    @Transactional
    public void addFavorite(Long userId, Long equipmentModelId) {
        User user = findActiveUser(userId);
        EquipmentModel equipmentModel = equipmentModelRepository.findByIdAndDeletedAtIsNull(equipmentModelId)
                .orElseThrow(() -> new EquipmentModelException(EquipmentModelErrorCode.MODEL_NOT_FOUND));

        if (favoriteRepository.existsByUserIdAndEquipmentModelId(userId, equipmentModelId)) {
            return;
        }

        favoriteRepository.save(Favorite.builder()
                .user(user)
                .equipmentModel(equipmentModel)
                .build());
    }

    @Override
    @Transactional
    public void removeFavorite(Long userId, Long equipmentModelId) {
        findActiveUser(userId);
        favoriteRepository.deleteByUserIdAndEquipmentModelId(userId, equipmentModelId);
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserException(UserErrorCode.INACTIVE_USER);
        }
        return user;
    }
}
