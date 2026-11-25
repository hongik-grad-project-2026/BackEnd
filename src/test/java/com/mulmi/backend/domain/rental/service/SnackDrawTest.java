package com.mulmi.backend.domain.rental.service;

import com.mulmi.backend.domain.equipment.repository.EquipmentItemRepository.ModelStockCount;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;

class SnackDrawTest {

    // 1등 1 · 2등 5 · 3등 17 · 꽝 15 (합 38) → 칸 [0] [1~5] [6~22] [23~37]
    private final List<ModelStockCount> stocks = List.of(
            stock(1L, 1), stock(2L, 5), stock(3L, 17), stock(4L, 15));

    @Test
    void 난수가_떨어진_칸의_경품을_뽑는다() {
        assertThat(SnackDraw.pick(stocks, fixed(0))).isEqualTo(1L);
        assertThat(SnackDraw.pick(stocks, fixed(1))).isEqualTo(2L);
        assertThat(SnackDraw.pick(stocks, fixed(5))).isEqualTo(2L);
        assertThat(SnackDraw.pick(stocks, fixed(6))).isEqualTo(3L);
        assertThat(SnackDraw.pick(stocks, fixed(22))).isEqualTo(3L);
        assertThat(SnackDraw.pick(stocks, fixed(23))).isEqualTo(4L);
        assertThat(SnackDraw.pick(stocks, fixed(37))).isEqualTo(4L);
    }

    @Test
    void 난수_범위는_남은_수량_합계다() {
        RandomGenerator random = new RandomGenerator() {
            @Override
            public long nextLong() {
                throw new UnsupportedOperationException();
            }

            @Override
            public long nextLong(long bound) {
                assertThat(bound).isEqualTo(38);
                return 0;
            }
        };
        SnackDraw.pick(stocks, random);
    }

    @Test
    void 경품_이름에서_등수와_이름을_나눈다() {
        assertThat(SnackDraw.rankOf("[2등] 새콤달콤")).isEqualTo("2등");
        assertThat(SnackDraw.displayName("[2등] 새콤달콤")).isEqualTo("새콤달콤");
        assertThat(SnackDraw.rankOf("[꽝]핵아이셔")).isEqualTo("꽝");
        assertThat(SnackDraw.displayName("[꽝]핵아이셔")).isEqualTo("핵아이셔");
        // 일반 간식 : 등수 없음, 이름 그대로
        assertThat(SnackDraw.rankOf("참크래커")).isNull();
        assertThat(SnackDraw.displayName("참크래커")).isEqualTo("참크래커");
    }

    // nextLong(bound)가 항상 value를 돌려주는 가짜 난수
    private static RandomGenerator fixed(long value) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                throw new UnsupportedOperationException();
            }

            @Override
            public long nextLong(long bound) {
                return value;
            }
        };
    }

    private static ModelStockCount stock(Long modelId, long available) {
        return new ModelStockCount() {
            public Long getModelId() { return modelId; }
            public Long getTotalCount() { return available; }
            public Long getAvailableCount() { return available; }
        };
    }
}
