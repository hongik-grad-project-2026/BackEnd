package com.mulmi.backend.domain.rental.stock;

import com.mulmi.backend.domain.equipment.repository.EquipmentItemRepository;
import com.mulmi.backend.domain.equipment.repository.EquipmentItemRepository.ModelStockCount;
import com.mulmi.backend.domain.equipment.repository.EquipmentModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 앱 시작 시 모델별 대여 가능 수를 DB 기준으로 Redis에 채운다
@Slf4j
@Component
@RequiredArgsConstructor
public class RentalStockInitializer implements ApplicationRunner {

    private final EquipmentModelRepository equipmentModelRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final RentalStockCounter rentalStockCounter;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<Long> modelIds = equipmentModelRepository.findAllActiveIds();
            if (modelIds.isEmpty()) {
                return; // 빈 목록을 IN ()으로 넘기지 않는다
            }

            // 기기가 한 대도 없는 모델은 집계 결과에 안 나오므로 0으로 먼저 채운다
            // (키 없음 = "Redis가 모름"으로 쓰기 위해 재고 0인 모델도 키를 만든다)
            Map<Long, Long> availableByModel = new HashMap<>();
            for (Long modelId : modelIds) {
                availableByModel.put(modelId, 0L);
            }
            for (ModelStockCount count : equipmentItemRepository.countStockByModelIds(modelIds)) {
                availableByModel.put(count.getModelId(), count.getAvailableCount());
            }

            availableByModel.forEach(rentalStockCounter::set);
            log.info("대여 재고 동기화 완료: 모델 {}개", availableByModel.size());
        } catch (DataAccessException exception) {
            // Redis(또는 DB) 실패로 앱 기동까지 막지 않는다. 키가 없으면 신청은 DB 잠금만으로 처리된다
            log.warn("대여 재고 동기화 실패", exception);
        }
    }
}
