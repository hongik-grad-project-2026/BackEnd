package com.mulmi.backend.domain.rental.stock;

import com.mulmi.backend.domain.equipment.repository.EquipmentItemRepository;
import com.mulmi.backend.domain.equipment.repository.EquipmentModelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RentalStockSynchronizerTest {

    @Mock
    private EquipmentModelRepository equipmentModelRepository;

    @Mock
    private EquipmentItemRepository equipmentItemRepository;

    @Mock
    private RentalStockCounter rentalStockCounter;

    @InjectMocks
    private RentalStockSynchronizer synchronizer;

    // 목록을 읽은 뒤 ~ 키 삭제 전에 폐기된 모델(900)은 SET 대상에서 빠진다 (폐기된 모델의 키를 되살리지 않음)
    @Test
    void modelRetiredBeforeKeyDeletionIsNotWrittenBack() {
        given(equipmentModelRepository.findAllActiveIds())
                .willReturn(List.of(900L, 901L))   // 키 삭제 전
                .willReturn(List.of(901L));        // 키 삭제 후 (900 폐기됨)
        given(equipmentItemRepository.countStockByModelIds(List.of(901L))).willReturn(List.of());

        synchronizer.scheduledResync();

        verify(rentalStockCounter).beginResync(List.of(900L, 901L));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Long, Long>> written = ArgumentCaptor.forClass(Map.class);
        verify(rentalStockCounter).finishResync(written.capture(), anyLong(), any(Duration.class));
        assertThat(written.getValue()).containsOnlyKeys(901L);
    }
}
