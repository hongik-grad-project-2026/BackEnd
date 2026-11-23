package com.mulmi.backend.domain.rental.stock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// 자바 쪽 순서 처리(진행 중 증감, 믿지 않음 표시)를 실행 순서를 강제해 검증한다. Redis는 모의 객체
// (누락 표시와 조건부 SET은 Redis 스크립트 안의 동작이라 실제 Redis로 따로 확인했다)
@ExtendWith(MockitoExtension.class)
class RentalStockCounterTest {

    private static final Long MODEL = 900L;
    private static final String KEY = "rental:stock:900";
    private static final String MISSED_KEY = "rental:stock:missed:900";
    private static final Duration TTL = Duration.ofMinutes(3);

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private RentalStockCounter counter;

    @BeforeEach
    void startTransactionSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTransactionSynchronization() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    // 폐기 커밋 → 재동기화가 폐기 반영된 1을 SET → 늦은 콜백 -1 → 0 (남은 1대가 품절로 거절) 을 막는다
    @Test
    void resyncSkipsModelWhoseCommittedChangeIsNotYetAppliedToRedis() {
        // 기기 폐기 트랜잭션 : 콜백 등록 후 커밋 직전까지 진행 (콜백은 아직)
        counter.addAfterCommit(MODEL, -1);
        TransactionSynchronization disposal = TransactionSynchronizationManager.getSynchronizations().get(0);
        disposal.beforeCommit(false);

        // 재동기화 : DB 집계는 이미 폐기가 반영된 1 → SET하지 않는다 (키 없음 → 신청은 DB로)
        long startedAt = counter.beginResync(List.of(MODEL));
        counter.finishResync(Map.of(MODEL, 1L), startedAt, TTL);
        verify(redisTemplate, never()).execute(any(RedisScript.class), eq(List.of(KEY, MISSED_KEY)), anyString(), anyString());

        // 늦은 콜백이 끝난 뒤의 재동기화는 SET한다
        disposal.afterCommit();
        disposal.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

        long nextStartedAt = counter.beginResync(List.of(MODEL));
        counter.finishResync(Map.of(MODEL, 1L), nextStartedAt, TTL);
        verify(redisTemplate).execute(any(RedisScript.class), eq(List.of(KEY, MISSED_KEY)), eq("1"), eq("180000"));
    }

    // 롤백되면 콜백은 실행되지 않고 진행 중 표시도 풀려, 재동기화가 막히지 않는다
    @Test
    void rolledBackChangeDoesNotBlockResync() {
        counter.addAfterCommit(MODEL, 1);
        TransactionSynchronization sync = TransactionSynchronizationManager.getSynchronizations().get(0);
        sync.beforeCommit(false);
        sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        long startedAt = counter.beginResync(List.of(MODEL));
        counter.finishResync(Map.of(MODEL, 2L), startedAt, TTL);

        verify(redisTemplate).execute(any(RedisScript.class), eq(List.of(KEY, MISSED_KEY)), eq("2"), eq("180000"));
    }

    // Redis 반영도 키 삭제도 실패하면(장애) DB로 판단하고, 재동기화 SET이 성공한 뒤에야 다시 Redis를 본다
    @Test
    void failedRedisUpdateSendsModelToDatabaseUntilResync() {
        given(redisTemplate.execute(any(RedisScript.class), eq(List.of(KEY, MISSED_KEY)), eq("1")))
                .willThrow(new RedisConnectionFailureException("down"));
        given(redisTemplate.execute(any(RedisScript.class), eq(List.of(KEY, MISSED_KEY))))
                .willThrow(new RedisConnectionFailureException("down"));

        counter.increase(MODEL);
        assertThat(counter.decrease(MODEL)).isNull();

        given(redisTemplate.execute(any(RedisScript.class), eq(List.of(KEY, MISSED_KEY)), eq("5"), eq("180000")))
                .willReturn(1L);
        long startedAt = counter.beginResync(List.of(MODEL));
        counter.finishResync(Map.of(MODEL, 5L), startedAt, TTL);

        given(redisTemplate.execute(any(RedisScript.class), eq(List.of(KEY)))).willReturn(4L);
        assertThat(counter.decrease(MODEL)).isEqualTo(4L);
    }

    // 차감이 타임아웃 나면(Redis는 이미 뺐을 수도 있음) 키를 지우고 누락 표시를 남겨 DB로 보낸다
    @Test
    void failedDecreaseInvalidatesKeyAndGoesToDatabase() {
        given(redisTemplate.execute(any(RedisScript.class), eq(List.of(KEY, MISSED_KEY)), eq("3"), eq("180000")))
                .willReturn(1L);
        long startedAt = counter.beginResync(List.of(MODEL));
        counter.finishResync(Map.of(MODEL, 3L), startedAt, TTL);
        given(redisTemplate.execute(any(RedisScript.class), eq(List.of(KEY))))
                .willThrow(new QueryTimeoutException("timeout"));

        assertThat(counter.decrease(MODEL)).isNull();
        verify(redisTemplate).execute(any(RedisScript.class), eq(List.of(KEY, MISSED_KEY)));
    }

    // 리뷰 재현 : 마지막 기기 폐기 커밋(-1 콜백 대기) → 신청이 Redis 1→0 후 DB 품절 → 반납 +1 → 늦은 폐기 -1
    //  뺀 1을 그대로 두면 DB 1, Redis 0이 된다 → 진행 중인 증감이 있었으므로 키를 무효화해야 한다
    @Test
    void databaseOutOfStockWithPendingChangeInvalidatesKey() {
        counter.addAfterCommit(MODEL, -1);
        TransactionSynchronization disposal = TransactionSynchronizationManager.getSynchronizations().get(0);
        disposal.beforeCommit(false);                       // 폐기 커밋됨, 콜백은 아직

        long token = counter.changeToken(MODEL);            // 신청 : 차감 전 표
        counter.onDatabaseOutOfStock(MODEL, token);         // DB 품절

        verify(redisTemplate).execute(any(RedisScript.class), eq(List.of(KEY, MISSED_KEY)));
    }

    // 표를 받은 뒤 새 증감이 시작돼 콜백까지 끝났어도(숫자가 컸던 원인이 그것일 수 있음) 무효화한다
    @Test
    void databaseOutOfStockAfterChangeBegunInvalidatesKey() {
        long token = counter.changeToken(MODEL);

        counter.addAfterCommit(MODEL, -1);
        TransactionSynchronization disposal = TransactionSynchronizationManager.getSynchronizations().get(0);
        disposal.beforeCommit(false);
        disposal.afterCommit();
        disposal.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

        counter.onDatabaseOutOfStock(MODEL, token);

        verify(redisTemplate).execute(any(RedisScript.class), eq(List.of(KEY, MISSED_KEY)));
    }

    // 늦게 올 증감이 없으면(재동기화 중 빠진 차감 등) 키를 지우지 않는다 : 뺀 1이 숫자를 실제로 내린다
    @Test
    void databaseOutOfStockWithoutPendingChangeKeepsKey() {
        long token = counter.changeToken(MODEL);

        counter.onDatabaseOutOfStock(MODEL, token);

        verify(redisTemplate, never()).execute(any(RedisScript.class), eq(List.of(KEY, MISSED_KEY)));
    }

    // 첫 재동기화 전에는 남아 있던 키를 믿지 않고 DB로 보낸다
    @Test
    void decreaseGoesToDatabaseBeforeFirstResync() {
        assertThat(counter.decrease(MODEL)).isNull();
        verify(redisTemplate, never()).execute(any(RedisScript.class), eq(List.of(KEY)));

        given(redisTemplate.execute(any(RedisScript.class), eq(List.of(KEY, MISSED_KEY)), eq("3"), eq("180000")))
                .willReturn(1L);
        long startedAt = counter.beginResync(List.of(MODEL));
        counter.finishResync(Map.of(MODEL, 3L), startedAt, TTL);
        given(redisTemplate.execute(any(RedisScript.class), eq(List.of(KEY)))).willReturn(2L);
        assertThat(counter.decrease(MODEL)).isEqualTo(2L);
    }

    // 이번 실행의 재동기화가 키를 쓰지 않은 모델(폐기돼 대상에서 빠짐, 누락 표시로 건너뜀)은
    //  Redis에 예전 키가 남아 있어도 보지 않고 DB로 보낸다 (폐기 모델이 404 대신 품절이 되는 것을 막음)
    @Test
    void modelNotWrittenByThisRunsResyncGoesToDatabase() {
        long startedAt = counter.beginResync(List.of(MODEL));
        counter.finishResync(Map.of(), startedAt, TTL);           // 폐기돼 활성 목록에서 빠짐

        assertThat(counter.decrease(MODEL)).isNull();
        verify(redisTemplate, never()).execute(any(RedisScript.class), eq(List.of(KEY)));
    }

    // 재동기화 SET이 누락 표시 때문에 건너뛰어지면(0) 믿지 않음 표시도 풀지 않는다
    @Test
    void skippedResyncKeepsModelOnDatabase() {
        given(redisTemplate.execute(any(RedisScript.class), eq(List.of(KEY, MISSED_KEY)), eq("1")))
                .willThrow(new RedisConnectionFailureException("down"));
        given(redisTemplate.execute(any(RedisScript.class), eq(List.of(KEY, MISSED_KEY))))
                .willThrow(new RedisConnectionFailureException("down"));
        counter.increase(MODEL);

        given(redisTemplate.execute(any(RedisScript.class), eq(List.of(KEY, MISSED_KEY)), eq("5"), eq("180000")))
                .willReturn(0L);
        long startedAt = counter.beginResync(List.of(MODEL));
        counter.finishResync(Map.of(MODEL, 5L), startedAt, TTL);

        assertThat(counter.decrease(MODEL)).isNull();
    }
}
