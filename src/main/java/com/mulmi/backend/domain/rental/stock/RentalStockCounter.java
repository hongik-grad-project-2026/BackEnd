package com.mulmi.backend.domain.rental.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

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

    // 모델의 대여 가능 수를 덮어쓴다 (DB가 기준일 때: 앱 시작, 기기 등록·폐기)
    public void set(Long modelId, long count) {
        redisTemplate.opsForValue().set(key(modelId), String.valueOf(count));
    }

    // null = Redis가 이 모델을 모름(DB에 맡김), -1 = 품절, 0 이상 = 통과
    public Long decrease(Long modelId) {
        return redisTemplate.execute(DECREASE_SCRIPT, List.of(key(modelId)));
    }

    // 재고가 1 돌아온다 (신청 실패 되돌리기, 취소·거절·노쇼·반납). 키가 없으면 아무것도 안 함
    public void increase(Long modelId) {
        redisTemplate.execute(INCREASE_SCRIPT, List.of(key(modelId)));
    }

    private String key(Long modelId) {
        return KEY_PREFIX + modelId;
    }
}