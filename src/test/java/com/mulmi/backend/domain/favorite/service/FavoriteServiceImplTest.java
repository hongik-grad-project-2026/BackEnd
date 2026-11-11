package com.mulmi.backend.domain.favorite.service;

import com.mulmi.backend.domain.equipment.entity.Category;
import com.mulmi.backend.domain.equipment.entity.EquipmentModel;
import com.mulmi.backend.domain.equipment.enums.EquipmentModelStatus;
import com.mulmi.backend.domain.equipment.repository.EquipmentItemRepository;
import com.mulmi.backend.domain.equipment.repository.EquipmentItemRepository.ModelStockCount;
import com.mulmi.backend.domain.equipment.repository.EquipmentModelRepository;
import com.mulmi.backend.domain.favorite.dto.response.FavoritePageResponseDTO;
import com.mulmi.backend.domain.favorite.entity.Favorite;
import com.mulmi.backend.domain.favorite.repository.FavoriteRepository;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.domain.user.enums.UserRole;
import com.mulmi.backend.domain.user.enums.UserStatus;
import com.mulmi.backend.domain.user.exception.UserException;
import com.mulmi.backend.domain.user.exception.code.UserErrorCode;
import com.mulmi.backend.domain.user.repository.UserRepository;
import com.mulmi.backend.global.storage.S3StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceImplTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EquipmentModelRepository equipmentModelRepository;

    @Mock
    private EquipmentItemRepository equipmentItemRepository;

    @Mock
    private S3StorageService s3StorageService;

    @InjectMocks
    private FavoriteServiceImpl favoriteService;

    @Test
    void addFavoriteSavesUserAndEquipmentModel() {
        User user = createActiveStudent();
        EquipmentModel model = createEquipmentModel();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(equipmentModelRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(model));

        favoriteService.addFavorite(1L, 10L);

        verify(favoriteRepository).insertIfAbsent(1L, 10L);
    }

    @Test
    void addFavoriteIsIdempotentWhenAlreadyFavorited() {
        User user = createActiveStudent();
        EquipmentModel model = createEquipmentModel();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(equipmentModelRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(model));
        given(favoriteRepository.insertIfAbsent(1L, 10L)).willReturn(0);

        favoriteService.addFavorite(1L, 10L);

        verify(favoriteRepository).insertIfAbsent(1L, 10L);
    }

    @Test
    void addFavoriteRejectsInactiveUser() {
        User suspended = User.builder()
                .id(1L)
                .loginId("C123456")
                .password("encoded-password")
                .name("학생")
                .email("student@example.com")
                .phoneNumber("01012345678")
                .role(UserRole.STUDENT)
                .status(UserStatus.SUSPENDED)
                .build();
        given(userRepository.findById(1L)).willReturn(Optional.of(suspended));

        assertThatThrownBy(() -> favoriteService.addFavorite(1L, 10L))
                .isInstanceOf(UserException.class)
                .extracting("code")
                .isEqualTo(UserErrorCode.INACTIVE_USER);
    }

    @Test
    void removeFavoriteIsIdempotent() {
        given(userRepository.findById(1L)).willReturn(Optional.of(createActiveStudent()));

        favoriteService.removeFavorite(1L, 10L);

        verify(favoriteRepository).deleteByUserIdAndEquipmentModelId(1L, 10L);
    }

    @Test
    void getMyFavoritesReturnsModelAndStockInformation() {
        User user = createActiveStudent();
        EquipmentModel model = createEquipmentModel();
        Favorite favorite = Favorite.builder()
                .id(100L)
                .user(user)
                .equipmentModel(model)
                .build();
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id"));
        ModelStockCount stock = org.mockito.Mockito.mock(ModelStockCount.class);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(favoriteRepository.findFavoritePageByUserId(1L, pageable))
                .willReturn(new PageImpl<>(List.of(favorite), pageable, 1));
        given(equipmentItemRepository.countStockByModelIds(List.of(10L)))
                .willReturn(List.of(stock));
        given(stock.getModelId()).willReturn(10L);
        given(stock.getAvailableCount()).willReturn(2L);
        given(stock.getTotalCount()).willReturn(3L);
        given(s3StorageService.createDownloadUrl("model-images/10/image.png"))
                .willReturn("https://example.com/image.png");

        FavoritePageResponseDTO result = favoriteService.getMyFavorites(1L, 0, 20);

        assertThat(result.favorites()).hasSize(1);
        assertThat(result.favorites().get(0).modelId()).isEqualTo(10L);
        assertThat(result.favorites().get(0).modelImageUrl()).isEqualTo("https://example.com/image.png");
        assertThat(result.favorites().get(0).availableCount()).isEqualTo(2L);
        assertThat(result.favorites().get(0).totalCount()).isEqualTo(3L);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
    }

    private User createActiveStudent() {
        return User.builder()
                .id(1L)
                .loginId("C123456")
                .studentId("C123456")
                .password("encoded-password")
                .name("학생")
                .email("student@example.com")
                .phoneNumber("01012345678")
                .role(UserRole.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private EquipmentModel createEquipmentModel() {
        Category category = Category.builder()
                .id(2L)
                .name("노트북")
                .build();
        return EquipmentModel.builder()
                .id(10L)
                .category(category)
                .name("MacBook Pro")
                .components("충전기")
                .description("대여용 노트북")
                .specification("M3")
                .modelImageUrl("model-images/10/image.png")
                .rentalStatus(EquipmentModelStatus.AVAILABLE)
                .disposalDate(LocalDate.of(2030, 12, 31))
                .build();
    }
}
