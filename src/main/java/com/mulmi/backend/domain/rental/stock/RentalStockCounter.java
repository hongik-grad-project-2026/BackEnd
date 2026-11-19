package com.mulmi.backend.domain.rental.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RentalStockCounter {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "rental:stock:";

    // 키가 있고 0보다 클 때만 1을 뺀다 (확인과 빼기가 Redis 안에서 한 번에 실행됨)
    // 결과: 키 없음 → nil, 재고 0 → -1, 그 외 → 뺀 뒤 남은 수
    private static final DefaultRedisScript<Long> DECREASE_SCRIPT = new DefaultRedisScript<>("""
            local stock = redis.call('GET', KEYS[1])
            if not stock then return false end
            if tonumber(stock) <= 0 then return -1 end
            return redis.call('DECR', KEYS[1])
            """, Long.class);

    // 키가 있을 때만 1을 더한다 (없는 키에 INCR하면 1이라는 가짜 재고가 생기므로)
    private static final DefaultRedisScript<Long> INCREASE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 0 then return false end
            return redis.call('INCR', KEYS[1])
            """, Long.class);

    // 키가 있을 때만 delta만큼 더한다 (음수면 빼기). 결과가 0 밑이면 0으로 둔다
    private static final DefaultRedisScript<Long> ADD_SCRIPT = new DefaultRedisScript<>("""
            local stock = redis.call('GET', KEYS[1])
            if not stock then return false end
            local result = tonumber(stock) + tonumber(ARGV[1])
            if result < 0 then result = 0 end
            redis.call('SET', KEYS[1], result)
            return result
            """, Long.class);

    // 모델의 대여 가능 수를 덮어쓴다 (DB가 기준일 때: 앱 시작)
    public void set(Long modelId, long count) {
        redisTemplate.opsForValue().set(key(modelId), String.valueOf(count));
    }

    // null = Redis가 이 모델을 모름(DB에 맡김), -1 = 품절, 0 이상 = 통과
    public Long decrease(Long modelId) {
        return redisTemplate.execute(DECREASE_SCRIPT, List.of(key(modelId)));
    }

    // 재고가 1 돌아온다 (신청 실패 되돌리기 — 트랜잭션 밖이라 바로 실행). 키가 없으면 아무것도 안 함
    public void increase(Long modelId) {
        redisTemplate.execute(INCREASE_SCRIPT, List.of(key(modelId)));
    }

    // 아래 셋은 현재 트랜잭션이 커밋된 뒤에 실행된다 (롤백되면 실행 안 됨)
    // 취소·반려·노쇼·반납 +1 / 기기 등록 +n / 상태 수정 ±1 / 기기 폐기 -1
    public void addAfterCommit(Long modelId, long delta) {
        if (delta == 0) {
            return;
        }
        afterCommit(() -> redisTemplate.execute(ADD_SCRIPT, List.of(key(modelId)), String.valueOf(delta)));
    }

    // 모델 등록 : 기기가 아직 없으니 0
    public void createAfterCommit(Long modelId) {
        afterCommit(() -> set(modelId, 0));
    }

    // 모델 폐기 : 키가 없으면 신청은 DB 확인으로 넘어가고, DB가 폐기된 모델을 거절한다
    public void deleteAfterCommit(Long modelId) {
        afterCommit(() -> redisTemplate.delete(key(modelId)));
    }

    // 커밋은 이미 끝났으므로 Redis 실패를 밖으로 던지지 않는다 (던지면 성공한 요청이 500이 됨)
    // 틀어진 숫자는 주기 재동기화가 바로잡는다
    private void afterCommit(Runnable redisWork) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    redisWork.run();
                } catch (RuntimeException e) {
                    log.warn("Redis 재고 반영 실패 (DB는 커밋됨)", e);
                }
            }
        });
    }

    private String key(Long modelId) {
        return KEY_PREFIX + modelId;
    }
}