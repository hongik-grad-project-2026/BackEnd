package com.mulmi.backend.domain.rental.service;

import com.mulmi.backend.domain.equipment.entity.Category;
import com.mulmi.backend.domain.equipment.entity.EquipmentItem;
import com.mulmi.backend.domain.equipment.entity.EquipmentModel;
import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;
import com.mulmi.backend.domain.equipment.enums.EquipmentModelStatus;
import com.mulmi.backend.domain.equipment.repository.EquipmentItemRepository;
import com.mulmi.backend.domain.equipment.repository.EquipmentItemRepository.ModelStockCount;
import com.mulmi.backend.domain.equipment.repository.EquipmentModelRepository;
import com.mulmi.backend.domain.rental.exception.RentalException;
import com.mulmi.backend.domain.rental.exception.code.RentalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 졸업전시(11-26~27) 간식 : 간식도 기자재처럼 46 대여 신청으로 받는다 (명세 64)
//  서버는 이름으로 간식을 구분하므로, 관리자 화면에서 아래 이름 그대로 등록해야 한다
//  전시가 끝나면 간식 카테고리와 함께 이 클래스도 걷어낸다
@Component
@RequiredArgsConstructor
public class SnackDraw {

    static final String SNACK_CATEGORY = "간식";
    // 학생에게 숨기는 카테고리. 모델 이름 앞에 [1등] ~ [꽝] 등수를 붙인다
    public static final String PRIZE_CATEGORY = "랜덤 간식 경품";
    // 간식 카테고리 안의 카드 하나. 자기 기기는 없고, 누르면 경품 카테고리에서 추첨한다
    static final String RANDOM_MODEL = "랜덤";

    // 1인 1개를 볼 카테고리 : 랜덤 당첨 대여는 경품 모델로 저장되므로 두 카테고리를 같이 본다
    static final List<String> ONE_PER_PERSON_CATEGORIES = List.of(SNACK_CATEGORY, PRIZE_CATEGORY);

    // 뽑은 경품의 기기를 다른 신청이 먼저 가져갔을 때 다시 뽑는 횟수
    private static final int MAX_ATTEMPTS = 5;

    // "[등수] 이름" : 1번 그룹 = 대괄호 안, 2번 그룹 = 뒤의 이름 (사이 공백은 버림)
    private static final Pattern RANKED_NAME = Pattern.compile("^\\[(.+?)\\]\\s*(.+)$");

    private final EquipmentModelRepository equipmentModelRepository;
    private final EquipmentItemRepository equipmentItemRepository;

    public boolean isSnack(Category category) {
        return SNACK_CATEGORY.equals(category.getName());
    }

    public boolean isPrize(Category category) {
        return PRIZE_CATEGORY.equals(category.getName());
    }

    public boolean isRandom(EquipmentModel model) {
        // 이름부터 본다 : 카테고리는 지연 로딩이라 먼저 보면 모델마다 카테고리 조회가 나간다 (32 목록)
        return RANDOM_MODEL.equals(model.getName()) && isSnack(model.getCategory());
    }

    // 랜덤 추첨 : 남은 수량에 비례해 경품 하나를 뽑고 그 기기 1개를 잠가서 돌려준다
    //  호출한 쪽(createRental)의 트랜잭션 안에서 돈다 → 잠금은 그 트랜잭션이 끝날 때 풀린다
    //  1등 하루 1개는 코드로 막지 않는다 : 1등은 1개만 등록하고 다음날 관리자가 1개 추가 (11-25 결정)
    EquipmentItem drawPrizeItem() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            // 1. 남은 경품과 수량 (READ_COMMITTED라 매번 커밋된 최신 수량을 읽는다)
            List<Long> prizeIds = equipmentModelRepository.findRentableIdsByCategoryName(PRIZE_CATEGORY);
            if (prizeIds.isEmpty()) {
                break;
            }
            List<ModelStockCount> stocks = equipmentItemRepository.countStockByModelIds(prizeIds).stream()
                    .filter(stock -> stock.getAvailableCount() > 0)
                    .toList();
            if (stocks.isEmpty()) {
                break;
            }

            // 2. 수량 비례로 하나
            Long prizeId = pick(stocks, ThreadLocalRandom.current());

            // 3. 모델 공유 잠금 → 기기 잠금 (일반 신청과 같은 순서 : 모델 → 기기)
            //    1단계에서 센 뒤 다른 신청이 마지막 기기를 가져갔으면 비어 있다 → 다시 뽑는다
            //    잠근 뒤 다시 확인 : 후보를 센 뒤 관리자가 대여 불가로 바꿨으면 그 경품은 건너뛰고 다시 뽑는다
            Optional<EquipmentItem> item = equipmentModelRepository.findWithShareLockByIdAndDeletedAtIsNull(prizeId)
                    .filter(prize -> prize.getRentalStatus() == EquipmentModelStatus.AVAILABLE
                            && isPrize(prize.getCategory()))
                    .flatMap(prize -> equipmentItemRepository
                            .findFirstByEquipmentModelIdAndStatusAndDeletedAtIsNullOrderByLabelNumberDescIdDesc(
                                    prize.getId(), EquipmentItemStatus.AVAILABLE));
            if (item.isPresent()) {
                return item.get();
            }
        }
        throw new RentalException(RentalErrorCode.OUT_OF_STOCK);
    }

    // 32번 '랜덤' 카드 수량 : 경품들의 대여 가능 수·전체 수 합계 (추첨과 같은 후보·같은 집계 쿼리)
    public PrizeStock prizeStock() {
        List<Long> prizeIds = equipmentModelRepository.findRentableIdsByCategoryName(PRIZE_CATEGORY);
        if (prizeIds.isEmpty()) {
            return new PrizeStock(0, 0);
        }
        List<ModelStockCount> stocks = equipmentItemRepository.countStockByModelIds(prizeIds);
        return new PrizeStock(
                stocks.stream().mapToLong(ModelStockCount::getAvailableCount).sum(),
                stocks.stream().mapToLong(ModelStockCount::getTotalCount).sum());
    }

    public record PrizeStock(long available, long total) {
    }

    // 경품 이름 "[2등] 새콤달콤" → 등수 "2등" / 이름 "새콤달콤". 일반 간식(대괄호 없음)은 등수 null, 이름 그대로
    static String rankOf(String modelName) {
        Matcher matcher = RANKED_NAME.matcher(modelName);
        return matcher.matches() ? matcher.group(1) : null;
    }

    static String displayName(String modelName) {
        Matcher matcher = RANKED_NAME.matcher(modelName);
        return matcher.matches() ? matcher.group(2) : modelName;
    }

    // 가중치 추첨 : 0 ~ (합계-1) 중 난수 r을 뽑고, 수량을 앞에서부터 더해 r을 처음 넘는 경품
    //  예) 1등 1 · 2등 5 · 3등 17 · 꽝 15 (합 38) → r=0이면 1등, 1~5면 2등, 6~22면 3등, 23~37이면 꽝
    static Long pick(List<ModelStockCount> stocks, RandomGenerator random) {
        long total = stocks.stream().mapToLong(ModelStockCount::getAvailableCount).sum();
        long r = random.nextLong(total);
        long cumulative = 0;
        for (ModelStockCount stock : stocks) {
            cumulative += stock.getAvailableCount();
            if (r < cumulative) {
                return stock.getModelId();
            }
        }
        throw new IllegalStateException("추첨 범위 계산 오류");
    }
}
