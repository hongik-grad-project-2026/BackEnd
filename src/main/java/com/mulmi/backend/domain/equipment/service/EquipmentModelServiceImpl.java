package com.mulmi.backend.domain.equipment.service;

import com.mulmi.backend.domain.equipment.dto.request.EquipmentModelCreateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.request.EquipmentModelUpdateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentModelUpdateResponseDTO;
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
import com.mulmi.backend.domain.equipment.entity.EquipmentItem;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        // 0. 접두사 입력 정리 + 날짜 순서 검사 (DB 조회 전에 끝낼 수 있는 검사)
        String prefix = normalizePrefixCode(dto.prefixCode());
        String modelName = dto.modelName().trim();
        validateDateRange(dto.receivedDate(), dto.disposalDate());

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
        // 3번 검사를 동시에 통과한 요청은 DB UNIQUE에서 막히므로 여기서 409로 바꾼다
        EquipmentModel model = EquipmentModelConverter.toEquipmentModel(dto, category, labelPrefix);
        try {
            equipmentModelRepository.saveAndFlush(model);
        } catch (DataIntegrityViolationException exception) {
            throw new EquipmentModelException(EquipmentModelErrorCode.DUPLICATE_MODEL);
        }

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

        // 3. 새 이미지 업로드 (기존 키는 삭제용으로 기억)
        String previousObjectKey = model.getModelImageUrl();
        String objectKey = s3StorageService.upload(image, "model-images/" + modelId);

        // 4. S3는 롤백되지 않으므로 파일 정리는 트랜잭션 결과를 보고 한다
        //    커밋 → 기존 파일 삭제 / 롤백 → 방금 올린 파일 삭제 / 결과 불명 → 둘 다 보존
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    deleteModelImageQuietly(previousObjectKey);
                } else if (status == STATUS_ROLLED_BACK) {
                    deleteModelImageQuietly(objectKey);
                }
            }
        });

        // 5. DB에는 새 키 반영 (커밋 시 UPDATE)
        model.updateModelImage(objectKey);

        return new EquipmentModelImageUploadResponseDTO(model.getId());
    }

    // S3 이미지 삭제 (실패해도 요청 결과에는 영향 없음, 로그만 남김)
    private void deleteModelImageQuietly(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            s3StorageService.delete(objectKey);
        } catch (RuntimeException exception) {
            log.warn("모델 이미지 삭제 실패: {}", objectKey, exception);
        }
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

    // 모델 수정 : READ_COMMITTED 이유는 기기 일괄 등록과 같음 (잠금 대기 후 기기 존재 검사가 최신 커밋을 봐야 함)
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public EquipmentModelUpdateResponseDTO updateEquipmentModel(Long modelId, EquipmentModelUpdateRequestDTO dto) {
        String prefix = normalizePrefixCode(dto.prefixCode());
        String modelName = dto.modelName().trim();
        validateDateRange(dto.receivedDate(), dto.disposalDate());

        // 1. 폐기되지 않은 모델 조회
        EquipmentModel model = findActiveModel(modelId);

        // 2. 카테고리 조회 + 접두사 규칙 (등록과 같음)
        Category category = categoryRepository.findByIdAndDeletedAtIsNull(dto.categoryId())
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));
        if (category.isLabelRequired() && prefix == null) {
            throw new EquipmentModelException(EquipmentModelErrorCode.LABEL_PREFIX_REQUIRED);
        }
        if (!category.isLabelRequired() && prefix != null) {
            throw new EquipmentModelException(EquipmentModelErrorCode.LABEL_PREFIX_NOT_ALLOWED);
        }
        LabelPrefix labelPrefix = null;
        if (prefix != null) {
            labelPrefix = labelPrefixRepository.findByCode(prefix)
                    .orElseThrow(() -> new LabelPrefixException(LabelPrefixErrorCode.LABEL_PREFIX_NOT_FOUND));
        }

        // 3. 카테고리·접두사가 바뀌면 : 실물 라벨이 붙은 기기가 있으면 막는다
        //    기존 접두사를 잠가서 같은 접두사로 진행 중인 일괄 등록이 끝난 뒤에 검사한다
        LabelPrefix currentPrefix = model.getLabelPrefix();
        boolean categoryChanged = !category.getId().equals(model.getCategory().getId());
        boolean prefixChanged = !Objects.equals(
                currentPrefix == null ? null : currentPrefix.getId(),
                labelPrefix == null ? null : labelPrefix.getId());
        if (categoryChanged || prefixChanged) {
            if (currentPrefix != null) {
                labelPrefixRepository.findWithLockById(currentPrefix.getId());
            }
            if (equipmentItemRepository.existsByEquipmentModelIdAndDeletedAtIsNull(model.getId())) {
                throw new EquipmentModelException(EquipmentModelErrorCode.LABEL_SETTING_LOCKED);
            }
        }

        // 4. 이름 중복 검사 (자기 자신 제외)
        if (equipmentModelRepository.existsByCategoryIdAndNameAndIdNot(category.getId(), modelName, model.getId())) {
            throw new EquipmentModelException(EquipmentModelErrorCode.DUPLICATE_MODEL);
        }

        // 5. 반영 : 4번을 동시에 통과한 요청은 DB UNIQUE에서 막히므로 flush로 앞당겨 409로 바꾼다
        model.update(category, labelPrefix, modelName, dto.components(), dto.description(),
                dto.specification(), dto.rentalStatus(), dto.receivedDate(), dto.disposalDate());
        try {
            equipmentModelRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new EquipmentModelException(EquipmentModelErrorCode.DUPLICATE_MODEL);
        }
        return new EquipmentModelUpdateResponseDTO(model.getId());
    }

    // 모델 폐기 : 이미 폐기된 모델을 409로 구분하려고 삭제 여부와 상관없이 조회한다
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void retireEquipmentModel(Long modelId) {
        EquipmentModel model = equipmentModelRepository.findById(modelId)
                .orElseThrow(() -> new EquipmentModelException(EquipmentModelErrorCode.MODEL_NOT_FOUND));
        if (model.getDeletedAt() != null) {
            throw new EquipmentModelException(EquipmentModelErrorCode.MODEL_ALREADY_RETIRED);
        }

        // 같은 접두사로 진행 중인 일괄 등록이 끝난 뒤에 기기를 확인한다 (잠금만 걸고 결과는 쓰지 않음)
        // 이후 일괄 등록은 잠금을 얻은 뒤 모델이 폐기된 것을 보고 멈춘다
        if (model.getLabelPrefix() != null) {
            labelPrefixRepository.findWithLockById(model.getLabelPrefix().getId());
        }

        // 살아 있는 기기를 전부 잠그며 가져와서, 그 목록으로 예약·대여 중인지 본다
        //  잠금 없이 확인하면 그 사이 대여 신청이 기기를 RESERVED로 바꿔도 폐기가 진행된다 (42 기기 폐기와 같은 문제)
        //  대여 신청도 기기 행을 잠그므로 둘이 한 줄로 선다
        List<EquipmentItem> items = equipmentItemRepository.findAllWithLockByEquipmentModelIdAndDeletedAtIsNull(modelId);
        if (items.stream().anyMatch(item -> EquipmentItemStatus.IN_USE.contains(item.getStatus()))) {
            throw new EquipmentModelException(EquipmentModelErrorCode.MODEL_IN_USE);
        }

        // 남은 기기(대여 가능·고장)도 함께 폐기 : 변경 감지로 기기마다 deleted_at UPDATE
        items.forEach(EquipmentItem::dispose);
        model.retire();
    }

    // 삭제되지 않은 모델 조회
    private EquipmentModel findActiveModel(Long modelId) {
        return equipmentModelRepository.findByIdAndDeletedAtIsNull(modelId)
                .orElseThrow(() -> new EquipmentModelException(EquipmentModelErrorCode.MODEL_NOT_FOUND));
    }

    // 입고일 ≤ 폐기일 (같은 날은 허용, 미래 입고일도 허용 — 미리 등록할 수 있음)
    private void validateDateRange(LocalDate receivedDate, LocalDate disposalDate) {
        if (receivedDate.isAfter(disposalDate)) {
            throw new EquipmentModelException(EquipmentModelErrorCode.INVALID_DATE_RANGE);
        }
    }

    // 모델 이미지 검증 : JPG/PNG/WEBP, 5MB 이하
    private void validateModelImage(MultipartFile image) {
        // Set.of는 contains(null)에서 NPE를 던지므로 null을 먼저 거른다
        if (image == null || image.isEmpty()
                || image.getContentType() == null
                || !MODEL_IMAGE_CONTENT_TYPES.contains(image.getContentType())) {
            throw new EquipmentModelException(EquipmentModelErrorCode.INVALID_MODEL_IMAGE);
        }
        if (image.getSize() > MAX_MODEL_IMAGE_SIZE) {
            throw new EquipmentModelException(EquipmentModelErrorCode.MODEL_IMAGE_TOO_LARGE);
        }
    }

}
