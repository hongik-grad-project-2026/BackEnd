package com.mulmi.backend.domain.equipment.service;

import com.mulmi.backend.domain.equipment.dto.response.DashboardCategoryUsageResponseDTO;
import com.mulmi.backend.domain.equipment.dto.response.DashboardIdleModelResponseDTO;
import com.mulmi.backend.domain.equipment.dto.response.DashboardModelUsageResponseDTO;
import com.mulmi.backend.domain.equipment.entity.Category;
import com.mulmi.backend.domain.equipment.repository.CategoryRepository;
import com.mulmi.backend.domain.equipment.repository.EquipmentItemRepository;
import com.mulmi.backend.domain.equipment.repository.EquipmentItemRepository.ModelUsageRow;
import com.mulmi.backend.domain.equipment.repository.EquipmentModelRepository;
import com.mulmi.backend.domain.equipment.repository.EquipmentModelRepository.ModelIdleRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    // 62 : 카테고리를 이름으로 고른다. 이름이 바뀌면 그 카테고리는 응답에서 빠진다 (노션 62 명세)
    private static final List<String> USAGE_CATEGORY_NAMES = List.of("노트북", "태블릿");
    private static final int TOP_N = 3;

    // 63 : 마지막 반납(또는 입고) 후 이 일수 이상 지나면 장기 미대여
    private static final long IDLE_DAYS_THRESHOLD = 14;
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    // 가동률 내림차순 → 대여 중 수 내림차순 → 모델 id 오름차순 (같은 값이어도 순서가 매번 같게)
    private static final Comparator<ModelUsageRow> USAGE_ORDER =
            Comparator.comparingDouble((ModelUsageRow row) -> (double) row.getRentedCount() / row.getTotalCount())
                    .reversed()
                    .thenComparing(ModelUsageRow::getRentedCount, Comparator.reverseOrder())
                    .thenComparing(ModelUsageRow::getModelId);

    private final CategoryRepository categoryRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final EquipmentModelRepository equipmentModelRepository;

    // 62. 모델 가동률 TOP3
    @Override
    public List<DashboardCategoryUsageResponseDTO> getModelUsage() {
        // 1. 대상 카테고리 : 모델이 없는 카테고리도 models = [] 로 응답에 나가야 하므로 따로 조회
        List<Category> categories =
                categoryRepository.findAllByNameInAndDeletedAtIsNullOrderByIdAsc(USAGE_CATEGORY_NAMES);

        // 2. 모델별 대여 중 수·보유 수 (보유 0인 모델은 쿼리에서 이미 빠져 0으로 나눌 일이 없다)
        List<ModelUsageRow> rows = equipmentItemRepository.countUsageByCategoryNames(USAGE_CATEGORY_NAMES);

        // 3. 카테고리마다 : 그 카테고리 줄만 → 정렬 → 앞에서 3개 → 응답 DTO
        List<DashboardCategoryUsageResponseDTO> result = new ArrayList<>();
        for (Category category : categories) {
            List<DashboardModelUsageResponseDTO> models = rows.stream()
                    .filter(row -> row.getCategoryId().equals(category.getId()))
                    .sorted(USAGE_ORDER)
                    .limit(TOP_N)
                    .map(row -> new DashboardModelUsageResponseDTO(
                            row.getModelName(), row.getRentedCount(), row.getTotalCount()))
                    .toList();
            result.add(new DashboardCategoryUsageResponseDTO(category.getName(), models));
        }
        return result;
    }

    // 63. 장기 미대여 모델
    @Override
    public List<DashboardIdleModelResponseDTO> getLongTermIdleModels() {
        LocalDate today = LocalDate.now(KOREA);

        // 쿼리가 "대여 가능 모델 + AVAILABLE 기기 있음 + 지금 RENTED 없음" 모델만 준다
        //  → 기준일(마지막 반납일, 없으면 입고일)부터 오늘까지 일수 → 14일 이상 → 일수 내림차순·모델 id 오름차순 → 3개
        //  기준일을 알 수 없는 모델(반납 기록·입고일 둘 다 없음)은 한 건 때문에 전체가 500이 되지 않게 뺀다
        return equipmentModelRepository.findIdleCandidates().stream()
                .filter(row -> baseDateOf(row) != null)
                .map(row -> new IdleModel(row, ChronoUnit.DAYS.between(baseDateOf(row), today)))
                .filter(idle -> idle.idleDays() >= IDLE_DAYS_THRESHOLD)
                .sorted(Comparator.comparingLong(IdleModel::idleDays).reversed()
                        .thenComparing(idle -> idle.row().getModelId()))
                .limit(TOP_N)
                .map(idle -> new DashboardIdleModelResponseDTO(
                        idle.row().getModelName(), idle.row().getCategoryName(), idle.idleDays()))
                .toList();
    }

    // 기준일 : 마지막 실제 반납일, 반납 기록이 한 번도 없으면 모델 입고일
    private LocalDate baseDateOf(ModelIdleRow row) {
        LocalDateTime lastReturnedAt = row.getLastReturnedAt();
        return lastReturnedAt == null ? row.getReceivedDate() : lastReturnedAt.toLocalDate();
    }

    // 정렬하려고 쿼리 한 줄과 계산한 일수를 잠깐 묶어 두는 상자 (이 클래스 안에서만 씀)
    private record IdleModel(ModelIdleRow row, long idleDays) {
    }
}
