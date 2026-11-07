package com.mulmi.backend.domain.rental.scheduler;

import com.mulmi.backend.domain.rental.enums.RentalStatus;
import com.mulmi.backend.domain.rental.service.AdminRentalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.BiPredicate;

// 노쇼 처리 (서버가 자동으로) + 기한 지난 승인 대기 자동 취소 (11-07 추가)
// 수령 기한은 항상 어느 날의 20:00이라 매일 20:01(한국 시간)에 돈다
//  - "오늘 기한"이 아니라 "기한이 지난 대여 전부"를 고르므로, 서버가 꺼져 하루를 건너뛰어도 다음 실행 때 처리된다
//  - 실행 시각은 rental.no-show-cron 설정으로 바꿀 수 있다 (로컬 테스트 때 짧게)
@Slf4j
@Component
@RequiredArgsConstructor
public class RentalNoShowScheduler {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    // 이 클래스와 다른 Bean이어야 markNoShow·cancelExpiredApplication의 @Transactional이 걸린다 (같은 클래스 안 호출은 프록시를 거치지 않음)
    private final AdminRentalService adminRentalService;

    // cron 6칸 : 초 분 시 일 월 요일 → 매일 20시 1분 0초
    // zone : 서버 컨테이너가 UTC라 한국 시간으로 못 박는다 (없으면 한국 시간 새벽 5시 1분에 돈다)
    @Scheduled(cron = "${rental.no-show-cron:0 1 20 * * *}", zone = "Asia/Seoul")
    public void handlePickupDeadline() {
        LocalDateTime now = LocalDateTime.now(KOREA);
        // 수령 대기인데 기한 지남 → 노쇼 (학생이 안 옴)
        processEach("노쇼", adminRentalService.findPickupExpiredIds(RentalStatus.READY_FOR_PICKUP, now),
                now, adminRentalService::markNoShow);
        // 승인 대기인데 기한 지남 → 자동 취소, 사유 "승인 지연" (조교가 기한까지 승인하지 않음)
        processEach("승인 지연 취소", adminRentalService.findPickupExpiredIds(RentalStatus.PENDING_APPROVAL, now),
                now, adminRentalService::cancelExpiredApplication);
    }

    // 후보를 한 건씩 처리 : 건마다 트랜잭션이 따로라 한 건이 실패해도 나머지는 계속
    // (실패한 건은 롤백되고 다음 실행 때 다시 후보가 된다)
    private void processEach(String job, List<Long> rentalIds, LocalDateTime now,
                             BiPredicate<Long, LocalDateTime> handler) {
        int done = 0;
        for (Long rentalId : rentalIds) {
            try {
                if (handler.test(rentalId, now)) {
                    done++;
                }
            } catch (Exception e) {
                log.error("[{}] 대여 {} 처리 실패", job, rentalId, e);
            }
        }
        log.info("[{}] 후보 {}건 중 {}건 처리", job, rentalIds.size(), done);
    }
}
