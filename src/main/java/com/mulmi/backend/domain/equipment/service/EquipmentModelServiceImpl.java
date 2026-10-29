package com.mulmi.backend.domain.equipment.service;

import com.mulmi.backend.domain.equipment.dto.request.EquipmentModelCreateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentModelCreateResponseDTO;
import com.mulmi.backend.domain.equipment.converter.EquipmentModelConverter;
import com.mulmi.backend.domain.equipment.dto.response.AdminEquipmentItemPageResponseDTO;
import com.mulmi.backend.domain.equipment.dto.response.AdminEquipmentItemSummaryResponseDTO;
import com.mulmi.backend.domain.equipment.dto.response.AdminEquipmentModelDetailResponseDTO;
import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;
import com.mulmi.backend.domain.equipment.dto.response.AdminEquipmentModelPageResponseDTO;
import com.mulmi.backend.domain.equipment.dto.response.AdminEquipmentModelSummaryResponseDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentModelImageUploadResponseDTO;
import com.mulmi.backend.domain.equipment.entity.Category;
import com.mulmi.backend.domain.equipment.entity.EquipmentModel;
import com.mulmi.backend.domain.equipment.entity.LabelPrefix;
import com.mulmi.backend.domain.equipment.exception.CategoryException;
import com.mulmi.backend.domain.equipment.exception.EquipmentModelException;
import com.mulmi.backend.domain.equipment.exception.LabelPrefixException;
import com.mulmi.backend.domain.equipment.exception.code.CategoryErrorCode;
import com.mulmi.backend.domain.equipment.exception.code.EquipmentModelErrorCode;
import com.mulmi.backend.domain.equipment.exception.code.LabelPrefixErrorCode;
import com.mulmi.backend.domain.equipment.repository.CategoryRepository;
import com.mulmi.backend.domain.equipment.repository.EquipmentItemRepository;
import com.mulmi.backend.domain.equipment.repository.EquipmentItemRepository.ModelStockCount;
import com.mulmi.backend.domain.equipment.repository.EquipmentItemRepository.StatusCount;
import com.mulmi.backend.domain.equipment.repository.EquipmentModelRepository;
import com.mulmi.backend.domain.equipment.repository.EquipmentModelRepository.CategoryModelCount;
import com.mulmi.backend.domain.equipment.repository.LabelPrefixRepository;
import com.mulmi.backend.global.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentModelServiceImpl implements EquipmentModelService {

    private static final long MAX_MODEL_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> MODEL_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final EquipmentModelRepository equipmentModelRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final CategoryRepository categoryRepository;
    private final LabelPrefixRepository labelPrefixRepository;
    private final S3StorageService s3StorageService;

    // 모델 등록
    @Override
    @Transactional
    public EquipmentModelCreateResponseDTO createEquipmentModel(EquipmentModelCreateRequestDTO dto) {
        // 0. 접두사 입력 정리
        String prefix = normalizePrefixCode(dto.prefixCode());
        String modelName = dto.modelName().trim();

        // 1. 카테고리 조회
        Category category = categoryRepository.findByIdAndDeletedAtIsNull(dto.categoryId())
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        // 2. 접두사 규칙 및 라벨 검사
        // 노트북/태블릿은 라벨(접두사)가 있어야함
        // 기타는 라벨(접두사) 모두 없음
        // 따라서 라벨이 필요한데 없는 경우와, 라벨이 필요없는데 있는 경우를 막아야 함
        if (category.isLabelRequired() && prefix == null) {
            throw new EquipmentModelException(EquipmentModelErrorCode.LABEL_PREFIX_REQUIRED);
        }
        if (!category.isLabelRequired() && prefix != null) {
            throw new EquipmentModelException(EquipmentModelErrorCode.LABEL_PREFIX_NOT_ALLOWED);
        }

        // 3. 중복 검사 (삭제된 모델 포함, DB UNIQUE와 같은 기준)
        if (equipmentModelRepository.existsByCategoryIdAndName(category.getId(), modelName)) {
            throw new EquipmentModelException(EquipmentModelErrorCode.DUPLICATE_MODEL);
        }

        // 4. 접두사가 있다면 조회 (기타 카테고리는 null)
        LabelPrefix labelPrefix = null;
        if (prefix != null) {
            labelPrefix = labelPrefixRepository.findByCode(prefix)
                    .orElseThrow(() -> new LabelPrefixException(LabelPrefixErrorCode.LABEL_PREFIX_NOT_FOUND));
        }

        // 5. 엔티티 생성 후 저장
        EquipmentModel model = EquipmentModelConverter.toEquipmentModel(dto, category, labelPrefix);
        equipmentModelRepository.save(model);

        // 6. 응답 DTO return
        return EquipmentModelConverter.toEquipmentModelCreateResponseDTO(model);
    }

    // 접두사 입력 정리 : 널, 빈칸이 들어오면 널로, 그 외에는 trim
    private String normalizePrefixCode(String prefixCode) {
        if (prefixCode == null || prefixCode.isBlank()) {
            return null;
        }
        return prefixCode.trim();
    }

    // 모델 이미지 업로드
    @Override
    @Transactional
    public EquipmentModelImageUploadResponseDTO uploadModelImage(Long modelId, MultipartFile image) {
        // 1. 모델 조회
        EquipmentModel model = findActiveModel(modelId);

        // 2. 이미지 검증 (형식, 용량)
        validateModelImage(image);

        // 3. 새 이미지 업로드 후 objectKey로 교체 (기존 키는 삭제용으로 기억)
        String previousObjectKey = model.getModelImageUrl();
        String objectKey = s3StorageService.upload(image, "model-images/" + modelId);
        model.updateModelImage(objectKey);

        // 4. 기존 이미지 삭제 (실패해도 업로드는 성공 처리)
        if (previousObjectKey != null && !previousObjectKey.isBlank()) {
            try {
                s3StorageService.delete(previousObjectKey);
            } catch (RuntimeException exception) {
                log.warn("기존 모델 이미지 삭제 실패: {}", previousObjectKey, exception);
            }
        }

        return new EquipmentModelImageUploadResponseDTO(model.getId());
    }

    // 관리자 모델 목록 조회
    @Override
    public AdminEquipmentModelPageResponseDTO getAdminEquipmentModels(
            Long categoryId, String keyword, int page, int size) {
        // 1. 모델 한 페이지 조회 (최신 등록순)
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<EquipmentModel> models = equipmentModelRepository.searchModels(
                categoryId, normalizeKeyword(keyword), pageable);

        // 2. 이번 페이지 모델들의 재고를 한 번에 집계 -> modelId로 꺼낼 수 있게 Map으로
        Map<Long, ModelStockCount> stockByModelId = countStock(models.getContent());

        // 3. 모델마다 DTO로 변환 (기기 없는 모델은 집계 결과에 없으므로 0 / 0)
        Page<AdminEquipmentModelSummaryResponseDTO> responsePage = models.map(model -> {
            ModelStockCount stock = stockByModelId.get(model.getId());
            return EquipmentModelConverter.toAdminEquipmentModelSummaryResponseDTO(
                    model,
                    s3StorageService.createDownloadUrl(model.getModelImageUrl()),
                    stock == null ? 0 : stock.getAvailableCount(),
                    stock == null ? 0 : stock.getTotalCount()
            );
        });

        // 4. 카테고리 탭 옆 숫자
        return AdminEquipmentModelPageResponseDTO.of(responsePage, countModelsByCategory());
    }

    // 관리자 모델 단건 조회
    @Override
    public AdminEquipmentModelDetailResponseDTO getAdminEquipmentModel(Long modelId) {
        return EquipmentModelConverter.toAdminEquipmentModelDetailResponseDTO(findActiveModel(modelId));
    }

    // 모델별 기기 목록 조회
    @Override
    public AdminEquipmentItemPageResponseDTO getAdminEquipmentItems(
            Long modelId, EquipmentItemStatus status, String keyword, int page, int size) {
        // 1. 모델 존재 확인 (없으면 404)
        findActiveModel(modelId);

        // 2. 기기 한 페이지 조회 (라벨 번호순)
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.ASC, "labelNumber").and(Sort.by(Sort.Direction.ASC, "id")));
        Page<AdminEquipmentItemSummaryResponseDTO> responsePage = equipmentItemRepository
                .searchItems(modelId, status, normalizeKeyword(keyword), pageable)
                .map(EquipmentModelConverter::toAdminEquipmentItemSummaryResponseDTO);

        // 3. 상태 탭 옆 숫자
        return AdminEquipmentItemPageResponseDTO.of(responsePage, countItemsByStatus(modelId));
    }

    // 상태별 기기 수 + "all" 합계 (기기가 없는 상태도 0으로 채움)
    private Map<String, Long> countItemsByStatus(Long modelId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("all", 0L);
        for (EquipmentItemStatus itemStatus : EquipmentItemStatus.values()) {
            counts.put(itemStatus.name(), 0L);
        }
        long total = 0;
        for (StatusCount count : equipmentItemRepository.countByStatus(modelId)) {
            counts.put(count.getStatus().name(), count.getCount());
            total += count.getCount();
        }
        counts.put("all", total);
        return counts;
    }

    // 모델 목록의 재고 집계 (빈 페이지면 쿼리 생략)
    private Map<Long, ModelStockCount> countStock(List<EquipmentModel> models) {
        if (models.isEmpty()) {
            return Map.of();
        }
        List<Long> modelIds = models.stream().map(EquipmentModel::getId).toList();
        return equipmentItemRepository.countStockByModelIds(modelIds).stream()
                .collect(Collectors.toMap(ModelStockCount::getModelId, stock -> stock));
    }

    // 카테고리별 모델 수 + "all" 합계
    private Map<String, Long> countModelsByCategory() {
        Map<String, Long> counts = new HashMap<>();
        long total = 0;
        for (CategoryModelCount count : equipmentModelRepository.countModelsByCategory()) {
            counts.put(String.valueOf(count.getCategoryId()), count.getModelCount());
            total += count.getModelCount();
        }
        counts.put("all", total);
        return counts;
    }

    // 빈 검색어는 조건 없음(null)으로
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    // 삭제되지 않은 모델 조회
    private EquipmentModel findActiveModel(Long modelId) {
        return equipmentModelRepository.findByIdAndDeletedAtIsNull(modelId)
                .orElseThrow(() -> new EquipmentModelException(EquipmentModelErrorCode.MODEL_NOT_FOUND));
    }

    // 모델 이미지 검증 : JPG/PNG/WEBP, 5MB 이하
    private void validateModelImage(MultipartFile image) {
        if (image == null || image.isEmpty()
                || !MODEL_IMAGE_CONTENT_TYPES.contains(image.getContentType())) {
            throw new EquipmentModelException(EquipmentModelErrorCode.INVALID_MODEL_IMAGE);
        }
        if (image.getSize() > MAX_MODEL_IMAGE_SIZE) {
            throw new EquipmentModelException(EquipmentModelErrorCode.MODEL_IMAGE_TOO_LARGE);
        }
    }

}
