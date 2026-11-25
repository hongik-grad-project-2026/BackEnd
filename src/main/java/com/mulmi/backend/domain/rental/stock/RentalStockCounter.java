package com.mulmi.backend.domain.rental.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Redis 숫자가 실제보다 크면 DB 잠금이 최종 판정하므로 안전하지만, 작으면 재고가 있어도 품절로 거절된다
// 그래서 숫자가 어긋났을 수 있는 모델은 키를 두지 않거나(→ 신청은 DB로) 믿지 않다가, 재동기화로 복구한다
@Slf4j
@Component
@RequiredArgsConstructor
public class RentalStockCounter {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "rental:stock:";
    // 키가 없는 동안 증감이 무시됐다는 표시. 재동기화는 이 표시가 있는 모델의 키를 만들지 않는다
    private static final String MISSED_PREFIX = "rental:stock:missed:";

    // 믿지 않는 모델 : Redis 장애로 키를 못 지웠거나(invalidate 실패), 그 뒤 증감을 건너뛴 모델. decrease가 Redis를 보지 않고 DB로 보낸다
    //  값 = 마지막으로 무효화에 실패했거나 증감을 건너뛴 시각(ns). 이 시각 뒤에 시작한 재동기화가 SET에 성공해야 풀린다
    //  시각은 항상 compute 계열 람다 안에서 잰다 : 밖에서 잰 시각을 늦게 쓰면 그 사이 기록된 더 새 시각을 과거로 덮는다
    private final Map<Long, Long> untrusted = new ConcurrentHashMap<>();
    // 커밋 직전부터 Redis 반영(콜백)이 끝날 때까지 진행 중인 증감 수 (모델별, 서버 1대 기준)
    //  재동기화는 이 수가 남은 모델을 SET하지 않는다 : DB 집계에 이미 들어간 변경을 늦은 콜백이 한 번 더 적용하는 것을 막기 위해
    private final Map<Long, Integer> inFlight = new ConcurrentHashMap<>();
    // 지금까지 시작된(커밋 직전에 도달한) 증감 수 (모델별, 줄지 않음). 신청 도중 새 증감이 시작됐는지 알아보는 데 쓴다
    private final Map<Long, Long> begun = new ConcurrentHashMap<>();
    // inFlight·begun을 함께 바꾸고 함께 읽게 한다 : 따로 읽으면 그 사이 시작된 증감을 놓칠 수 있다
    private final Object changeLock = new Object();
    // 이번 실행의 재동기화가 키를 직접 쓴 모델만 Redis로 판단한다
    //  Redis에는 지난 실행의 키가 남아 있을 수 있다 : 앞 실행이 커밋 직후 콜백 전에 죽었다면 실제보다 작고,
    //  폐기 커밋 뒤 키 삭제 전에 죽었다면 폐기된 모델 키가 남는다(재동기화는 활성 모델만 다루므로 지워지지 않음 → 404 대신 품절)
    //  웹 서버는 ApplicationRunner보다 먼저 요청을 받으므로, 첫 재동기화 전에는 모든 신청이 DB로 간다
    private final Set<Long> syncedModels = ConcurrentHashMap.newKeySet();

    // 키가 있고 0보다 클 때만 1을 뺀다 (확인과 빼기가 Redis 안에서 한 번에 실행됨)
    // 결과: 키 없음 → nil, 재고 0 → -1, 그 외 → 뺀 뒤 남은 수
    private static final DefaultRedisScript<Long> DECREASE_SCRIPT = new DefaultRedisScript<>("""
            local stock = redis.call('GET', KEYS[1])
            if not stock then return false end
            if tonumber(stock) <= 0 then return -1 end
            return redis.call('DECR', KEYS[1])
            """, Long.class);

    // 키가 있으면 delta만큼 더한다 (음수면 빼기, 0 밑이면 0). KEEPTTL : 그냥 SET하면 만료 시간이 지워진다 (Redis 6.0+)
    // 키가 없으면 증감을 버리는 대신 "누락" 표시를 남긴다 → 같은 Redis 안에서 재동기화 SET보다 먼저/나중이 확정된다
    private static final DefaultRedisScript<Long> ADD_SCRIPT = new DefaultRedisScript<>("""
            local stock = redis.call('GET', KEYS[1])
            if not stock then
                redis.call('SET', KEYS[2], 1, 'EX', 600)
                return false
            end
            local result = tonumber(stock) + tonumber(ARGV[1])
            if result < 0 then result = 0 end
            redis.call('SET', KEYS[1], result, 'KEEPTTL')
            return result
            """, Long.class);

    // 재동기화 SET : 누락 표시가 있으면 쓰지 않는다 (집계 뒤에 무시된 증감이 있었다는 뜻) → 1 = 씀, 0 = 건너뜀
    private static final DefaultRedisScript<Long> RESYNC_SET_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[2]) == 1 then return 0 end
            redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
            return 1
            """, Long.class);

    // 키 삭제 + 누락 표시를 한 번에 : 진행 중인 재동기화가 앞서 읽은 집계값으로 키를 되살리지 못하게 한다
    private static final DefaultRedisScript<Long> INVALIDATE_SCRIPT = new DefaultRedisScript<>("""
            redis.call('DEL', KEYS[1])
            redis.call('SET', KEYS[2], 1, 'EX', 600)
            return 1
            """, Long.class);

    // null = Redis가 이 모델을 모르거나 믿을 수 없음(DB에 맡김), -1 = 품절, 0 이상 = 통과
    // 예외를 던지지 않는다 : 실패하면 이 모델을 DB 판단으로 돌리고 null
    public Long decrease(Long modelId) {
        if (!syncedModels.contains(modelId) || untrusted.containsKey(modelId)) {
            return null;
        }
        try {
            return redisTemplate.execute(DECREASE_SCRIPT, List.of(key(modelId)));
        } catch (RuntimeException e) {
            // 타임아웃은 Redis가 1을 뺀 뒤 응답만 늦은 것일 수도 있다. 그대로 DB로 보내면 아무도 되돌리지 않아
            // 숫자가 실제보다 1 작게 남는다(품절 오판) → 줄었는지 알 수 없는 숫자는 지우고 재동기화에 맡긴다
            log.warn("Redis 재고 차감 실패, DB로 판단 전환: modelId={}", modelId, e);
            invalidate(modelId);
            return null;
        }
    }

    // 신청 직전(decrease 전)에 받아 두는 표 : 진행 중인 증감이 있으면 -1, 없으면 지금까지 시작된 증감 수
    public long changeToken(Long modelId) {
        synchronized (changeLock) {
            return inFlight.containsKey(modelId) ? -1 : begun.getOrDefault(modelId, 0L);
        }
    }

    // Redis를 통과한 신청을 DB가 품절로 거절함 = Redis 숫자가 실제보다 컸다
    //  - 커지게 한 증감의 콜백이 아직 안 왔을 수 있음(예: 폐기 커밋 후 -1 대기) → 뺀 1을 두면 늦은 -1과 겹쳐 두 번 빠진다
    //    → 표를 받을 때 진행 중이었거나(-1) 그 뒤 새 증감이 시작됐으면 키를 무효화해 재동기화에 맡긴다
    //  - 그런 증감이 없었다면 바로잡는 -1은 오지 않는다(재동기화 중 빠진 차감 등) → 뺀 1을 그대로 두어 숫자를 내린다
    //    (오픈 몰림 중에도 키를 지우지 않아, 품절 요청이 계속 Redis에서 거절된다)
    public void onDatabaseOutOfStock(Long modelId, long tokenBeforeDecrease) {
        // 지금 진행 중이면 -1이 되어 표와 달라진다
        if (tokenBeforeDecrease < 0 || changeToken(modelId) != tokenBeforeDecrease) {
            invalidate(modelId);
        }
    }

    // 신청 실패 되돌리기 (트랜잭션 밖이라 바로 실행). 실패하면 이 모델을 믿지 않는다
    public void increase(Long modelId) {
        apply(modelId, 1);
    }

    // 현재 트랜잭션이 커밋된 뒤에 실행 (롤백되면 실행 안 됨)
    // 취소·반려·노쇼·반납 +1 / 기기 등록 +n / 상태 수정 ±1 / 기기 폐기 -1
    // 허용한 틈 (11-23 결정) : 커밋 ~ 콜백 사이 수 ms 동안은 +1이 아직 안 들어가 숫자가 실제보다 작다
    //  그 사이 같은 모델 신청은 품절로 거절될 수 있다 → 다시 누르면 통과. 잘못 빌려주는 일은 없다
    //  (커밋 전에 +1을 먼저 하는 방법은 재동기화·진행 중 표시와 얽혀 경쟁 조건이 늘어나 택하지 않음)
    // 허용한 오차 (11-23 결정) : 신청이 차감한 직후 재동기화가 새 키를 쓰고, 그 신청이 품절이 아닌 이유로 실패하면
    //  되돌리기 +1이 새 키에 더해져 숫자가 실제보다 1 크다 → 큰 쪽이라 DB 잠금이 막고,
    //  첫 DB 품절(onDatabaseOutOfStock이 뺀 1을 유지)이나 다음 재동기화가 바로잡는다
    //  (키 세대를 두어 막는 방법은 스크립트 세 개·Facade가 함께 바뀌어 택하지 않음)
    public void addAfterCommit(Long modelId, long delta) {
        if (delta == 0) {
            return;
        }
        afterCommit(modelId, () -> apply(modelId, delta));
    }

    // 모델 폐기 : 키가 없으면 신청은 DB 확인으로 넘어가고, DB가 폐기된 모델을 거절한다
    // (모델 등록 때는 키를 만들지 않는다 — 등록 커밋과 키 생성 사이에 들어온 기기 +n이 0에 덮일 수 있어서.
    //  새 모델은 키가 없어 DB로 처리되다가 다음 재동기화 때 DB 개수로 키가 생긴다)
    public void deleteAfterCommit(Long modelId) {
        afterCommit(modelId, () -> invalidate(modelId));
    }

    // ---- 재동기화 (RentalStockSynchronizer가 호출) ----
    // 순서 : 키·누락 표시 삭제 → 활성 모델 다시 조회·DB 집계 → 모델마다 조건부 SET
    // 키를 지우거나 SET을 막는 쪽(증감 실패 복구·모델 폐기·키 없는 증감)은 모두 누락 표시를 남기므로 조건부 SET이 막는다
    //  - 키 삭제 ~ SET 사이 신청 : 키가 없으므로 DB로 처리
    //  - 키 삭제 ~ SET 사이 증감 : 누락 표시가 남아 SET을 건너뜀 (집계에 들어갔는지 알 수 없으므로) → 키 없이 DB로, 다음 주기에 다시
    //  - 커밋은 집계 전, 콜백은 SET 뒤 : 늦은 콜백이 한 번 더 적용됨 → SET 직전 inFlight가 남은 모델은 건너뜀
    //  - 믿지 않는 모델의 증감은 Redis를 부르지 않아 누락 표시가 없다 → SET은 되지만, 믿지 않음 시각이 이 재동기화 시작 뒤로
    //    갱신돼 있으면 SET 뒤에도 계속 DB로 판단한다 (다음 주기에 다시)
    // 반환 : 시작 시각. 마지막 믿지 않음 시각이 이보다 앞선 모델만 SET 성공 후 다시 믿는다
    public long beginResync(List<Long> modelIds) {
        long startedAt = System.nanoTime();
        List<String> keys = new ArrayList<>();
        for (Long modelId : modelIds) {
            keys.add(key(modelId));
            keys.add(missedKey(modelId));
        }
        redisTemplate.delete(keys);
        return startedAt;
    }

    // ttl : 재동기화가 활성 모델 키를 매번 새로 쓰므로, 폐기된 모델의 키나 재동기화가 멈춘 뒤의 키만 만료된다
    // 반환 : 키를 쓴 모델 수 (나머지는 진행 중 증감·누락 표시로 건너뜀 → 키 없이 DB로)
    public int finishResync(Map<Long, Long> availableByModel, long startedAt, Duration ttl) {
        int writtenCount = 0;
        for (Map.Entry<Long, Long> entry : availableByModel.entrySet()) {
            Long modelId = entry.getKey();
            if (inFlight.containsKey(modelId)) {
                continue; // 키 없이 DB로, 다음 재동기화에서 다시 맞춘다
            }
            Long written = redisTemplate.execute(RESYNC_SET_SCRIPT,
                    List.of(key(modelId), missedKey(modelId)),
                    String.valueOf(entry.getValue()), String.valueOf(ttl.toMillis()));
            if (written != null && written == 1) {
                syncedModels.add(modelId);
                untrusted.computeIfPresent(modelId, (id, since) -> since < startedAt ? null : since);
                writtenCount++;
            }
        }
        return writtenCount;
    }

    // 증감 실행. 실패하면 숫자가 실제와 어긋났으므로 이 모델을 믿지 않는다
    private void apply(Long modelId, long delta) {
        // 믿지 않는 모델은 Redis를 부르지 않는다 : Redis가 멈춰 있으면 부를 때마다 타임아웃(약 1.2초)을 기다린다
        //  건너뛴 증감은 누락 표시를 남기지 않고, 진행 중인 재동기화의 집계에서 빠졌을 수 있다
        //  → 믿지 않음 시각을 지금으로 갱신해, 그 재동기화가 SET에 성공해도 이 모델을 계속 믿지 않게 한다
        //  (이 모델은 시작 뒤 건너뛴 증감이 없고 SET에 성공한 재동기화에서 풀린다)
        if (untrusted.computeIfPresent(modelId, (id, since) -> System.nanoTime()) != null) {
            return;
        }
        try {
            redisTemplate.execute(ADD_SCRIPT, List.of(key(modelId), missedKey(modelId)), String.valueOf(delta));
        } catch (RuntimeException e) {
            log.warn("Redis 재고 반영 실패, DB로 판단 전환: modelId={}, delta={}", modelId, delta, e);
            invalidate(modelId);
        }
    }

    // 키를 지우고 누락 표시를 남겨 DB 경로로 보낸다. 이것마저 실패하면(Redis 장애) 이 앱에서는 믿지 않는다
    private void invalidate(Long modelId) {
        try {
            redisTemplate.execute(INVALIDATE_SCRIPT, List.of(key(modelId), missedKey(modelId)));
        } catch (RuntimeException e) {
            log.warn("Redis 재고 키 삭제 실패, 재동기화까지 DB로 판단: modelId={}", modelId);
            untrusted.compute(modelId, (id, previous) -> System.nanoTime());
        }
    }

    // 커밋 직전 inFlight +1 → 커밋 → (커밋됐으면) Redis 반영 → inFlight -1. 롤백돼도 -1
    // 커밋은 이미 끝났으므로 Redis 실패를 밖으로 던지지 않는다 (redisWork 안에서 잡음, 던지면 성공한 요청이 500이 됨)
    private void afterCommit(Long modelId, Runnable redisWork) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            private boolean counted = false;

            @Override
            public void beforeCommit(boolean readOnly) {
                synchronized (changeLock) {
                    inFlight.merge(modelId, 1, Integer::sum);
                    begun.merge(modelId, 1L, Long::sum);
                }
                counted = true;
            }

            @Override
            public void afterCommit() {
                redisWork.run();
            }

            @Override
            public void afterCompletion(int status) {
                if (counted) {
                    synchronized (changeLock) {
                        inFlight.computeIfPresent(modelId, (id, count) -> count == 1 ? null : count - 1);
                    }
                }
            }
        });
    }

    private String key(Long modelId) {
        return KEY_PREFIX + modelId;
    }

    private String missedKey(Long modelId) {
        return MISSED_PREFIX + modelId;
    }
}
