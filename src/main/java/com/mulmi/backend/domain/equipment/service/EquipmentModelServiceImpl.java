package com.mulmi.backend.domain.equipment.service;

import com.mulmi.backend.domain.equipment.dto.request.EquipmentModelCreateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentModelCreateResponseDTO;
import com.mulmi.backend.domain.equipment.converter.EquipmentModelConverter;
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
import com.mulmi.backend.domain.equipment.repository.EquipmentModelRepository;
import com.mulmi.backend.domain.equipment.repository.LabelPrefixRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentModelServiceImpl implements EquipmentModelService {

    private final EquipmentModelRepository equipmentModelRepository;
    private final CategoryRepository categoryRepository;
    private final LabelPrefixRepository labelPrefixRepository;

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

}
