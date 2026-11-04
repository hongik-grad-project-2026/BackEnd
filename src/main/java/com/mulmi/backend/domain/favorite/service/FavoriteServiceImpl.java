package com.mulmi.backend.domain.favorite.service;

import com.mulmi.backend.domain.equipment.entity.EquipmentModel;
import com.mulmi.backend.domain.equipment.exception.EquipmentModelException;
import com.mulmi.backend.domain.equipment.exception.code.EquipmentModelErrorCode;
import com.mulmi.backend.domain.equipment.repository.EquipmentItemRepository;
import com.mulmi.backend.domain.equipment.repository.EquipmentItemRepository.ModelStockCount;
import com.mulmi.backend.domain.equipment.repository.EquipmentModelRepository;
import com.mulmi.backend.domain.favorite.dto.response.FavoriteModelResponseDTO;
import com.mulmi.backend.domain.favorite.dto.response.FavoritePageResponseDTO;
import com.mulmi.backend.domain.favorite.entity.Favorite;
import com.mulmi.backend.domain.favorite.repository.FavoriteRepository;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.domain.user.enums.UserStatus;
import com.mulmi.backend.domain.user.exception.UserException;
import com.mulmi.backend.domain.user.exception.code.UserErrorCode;
import com.mulmi.backend.domain.user.repository.UserRepository;
import com.mulmi.backend.global.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final EquipmentModelRepository equipmentModelRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final S3StorageService s3StorageService;

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

    @Override
    public FavoritePageResponseDTO getMyFavorites(Long userId, int page, int size) {
        findActiveUser(userId);

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Favorite> favorites = favoriteRepository.findFavoritePageByUserId(userId, pageable);

        Map<Long, ModelStockCount> stockByModelId = countStock(favorites.getContent());
        Page<FavoriteModelResponseDTO> responsePage = favorites.map(favorite -> {
            EquipmentModel model = favorite.getEquipmentModel();
            ModelStockCount stock = stockByModelId.get(model.getId());
            return new FavoriteModelResponseDTO(
                    model.getId(),
                    s3StorageService.createDownloadUrl(model.getModelImageUrl()),
                    model.getName(),
                    model.getCategory().getId(),
                    model.getCategory().getName(),
                    model.getComponents(),
                    model.getSpecification(),
                    model.getDescription(),
                    stock == null ? 0 : stock.getAvailableCount(),
                    stock == null ? 0 : stock.getTotalCount(),
                    model.getRentalStatus(),
                    model.getDisposalDate(),
                    favorite.getCreatedAt()
            );
        });

        return FavoritePageResponseDTO.from(responsePage);
    }

    private Map<Long, ModelStockCount> countStock(List<Favorite> favorites) {
        if (favorites.isEmpty()) {
            return Map.of();
        }
        List<Long> modelIds = favorites.stream()
                .map(favorite -> favorite.getEquipmentModel().getId())
                .toList();
        return equipmentItemRepository.countStockByModelIds(modelIds).stream()
                .collect(Collectors.toMap(ModelStockCount::getModelId, stock -> stock));
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
