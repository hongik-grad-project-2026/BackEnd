package com.mulmi.backend.domain.rental.stock;

import com.mulmi.backend.domain.equipment.repository.EquipmentItemRepository;
import com.mulmi.backend.domain.equipment.repository.EquipmentItemRepository.ModelStockCount;
import com.mulmi.backend.domain.equipment.repository.EquipmentModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 모델별 대여 가능 수를 DB 기준으로 Redis에 다시 채운다
//  - 앱 시작 시 한 번 + 주기적으로 : 커밋 후 반영이 실패했거나 무시된 증감을 복구한다
//  - 새로 등록된 모델의 키도 여기서 처음 생긴다 (그 전까지는 키가 없어 DB로 처리)
@Slf4j
@Component
@RequiredArgsConstructor
public class RentalStockSynchronizer implements ApplicationRunner {

    private final EquipmentModelRepository equipmentModelRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final RentalStockCounter rentalStockCounter;

    @Value("${rental.stock-resync-ms:60000}")
    private long resyncMs;

    // 직전 재동기화가 성공했는지 (resync는 synchronized라 한 번에 한 스레드만 바꾼다)
    private boolean lastResyncSucceeded = false;

    @Override
    public void run(ApplicationArguments args) {
        resync();
    }

    // fixedDelay : 앞 실행이 끝난 뒤부터 잰다 (겹쳐 돌지 않음). 첫 실행은 앱 시작 동기화 뒤로 미룬다
    @Scheduled(initialDelayString = "${rental.stock-resync-ms:60000}",
               fixedDelayString = "${rental.stock-resync-ms:60000}")
    public void scheduledResync() {
        resync();
    }

    // 앱 시작(main 스레드)과 스케줄러 스레드가 동시에 돌지 않게 한다
    private synchronized void resync() {
        List<Long> modelIds;
        try {
            modelIds = equipmentModelRepository.findAllActiveIds();
        } catch (RuntimeException exception) {
            lastResyncSucceeded = false;
            log.warn("대여 재고 재동기화 실패 (모델 조회)", exception);
            return;
        }
        if (modelIds.isEmpty()) {
            return; // 빈 목록을 IN ()으로 넘기지 않는다
        }

        long startedAt;
        try {
            startedAt = rentalStockCounter.beginResync(modelIds);
        } catch (RuntimeException exception) {
            lastResyncSucceeded = false;
            log.warn("대여 재고 재동기화 실패 (Redis 키 삭제), 다음 주기에 다시 시도", exception);
            return;
        }

        try {
            // 활성 모델을 키 삭제 뒤에 다시 읽는다 : 처음 목록을 읽은 뒤 ~ 키 삭제 전에 폐기된 모델은
            //  누락 표시도 키 삭제에 같이 지워져 조건부 SET이 막지 못하므로, 여기서 목록에서 뺀다
            //  (키 삭제 뒤에 폐기되면 누락 표시가 남아 조건부 SET이 건너뛴다)
            List<Long> activeIds = equipmentModelRepository.findAllActiveIds();
            if (activeIds.isEmpty()) {
                return;
            }
            // 기기가 한 대도 없는 모델은 집계 결과에 안 나오므로 0으로 먼저 채운다
            Map<Long, Long> availableByModel = new HashMap<>();
            for (Long modelId : activeIds) {
                availableByModel.put(modelId, 0L);
            }
            for (ModelStockCount count : equipmentItemRepository.countStockByModelIds(activeIds)) {
                availableByModel.put(count.getModelId(), count.getAvailableCount());
            }
            // 키 만료 = 주기의 3배 : 재동기화가 두 번 연속 실패해도 살아 있는 키는 유지된다
            int written = rentalStockCounter.finishResync(availableByModel, startedAt, Duration.ofMillis(resyncMs * 3));
            // 앱 시작 후 처음 키를 쓴 주기·실패 뒤 회복은 INFO로 남긴다 (배포 후 "Redis로 판단 중인지" 로그로 확인)
            //  평소엔 1분마다라 DEBUG. 키를 하나도 못 쓴 주기(전부 진행 중 증감·누락 표시)는 성공으로 치지 않는다
            if (written > 0 && !lastResyncSucceeded) {
                log.info("대여 재고 재동기화 완료: 모델 {}개 중 {}개 기록 (나머지는 다음 주기)", availableByModel.size(), written);
            } else {
                log.debug("대여 재고 재동기화 완료: 모델 {}개 중 {}개 기록", availableByModel.size(), written);
            }
            if (written > 0) {
                lastResyncSucceeded = true;
            }
        } catch (RuntimeException exception) {
            // 키는 이미 지워졌으므로 신청은 DB로 처리된다. 다음 주기에 다시 채운다
            lastResyncSucceeded = false;
            log.warn("대여 재고 재동기화 실패, 다음 주기에 다시 시도", exception);
        }
    }
}
