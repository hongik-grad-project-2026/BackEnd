package com.mulmi.backend.domain.equipment.service;

import com.mulmi.backend.domain.equipment.dto.request.EquipmentItemCreateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentItemCreateResponseDTO;
import com.mulmi.backend.domain.equipment.entity.EquipmentItem;
import com.mulmi.backend.domain.equipment.entity.EquipmentModel;
import com.mulmi.backend.domain.equipment.entity.LabelPrefix;
import com.mulmi.backend.domain.equipment.exception.EquipmentItemException;
import com.mulmi.backend.domain.equipment.exception.EquipmentModelException;
import com.mulmi.backend.domain.equipment.exception.LabelPrefixException;
import com.mulmi.backend.domain.equipment.exception.code.EquipmentItemErrorCode;
import com.mulmi.backend.domain.equipment.exception.code.EquipmentModelErrorCode;
import com.mulmi.backend.domain.equipment.exception.code.LabelPrefixErrorCode;
import com.mulmi.backend.domain.equipment.repository.EquipmentItemRepository;
import com.mulmi.backend.domain.equipment.repository.EquipmentModelRepository;
import com.mulmi.backend.domain.equipment.repository.LabelPrefixRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentItemServiceImpl implements EquipmentItemService {

    private final EquipmentItemRepository equipmentItemRepository;
    private final EquipmentModelRepository equipmentModelRepository;
    private final LabelPrefixRepository labelPrefixRepository;

    // READ_COMMITTED : MySQL 기본(REPEATABLE READ)이면 잠금을 기다린 뒤의 중복 검사가
    // 트랜잭션 첫 조회 시점 스냅샷을 읽어 앞 요청이 커밋한 라벨을 보지 못한다
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public EquipmentItemCreateResponseDTO createEquipmentItems(EquipmentItemCreateRequestDTO dto) {
        // 1. 폐기되지 않은 모델 조회
        EquipmentModel model = equipmentModelRepository.findByIdAndDeletedAtIsNull(dto.modelId())
                .orElseThrow(()-> new EquipmentModelException(EquipmentModelErrorCode.MODEL_NOT_FOUND));

        // 2. 카테고리 라벨 사용 여부로 분기 + labelStart 필수/금지 검사
        boolean labelRequired = model.getCategory().isLabelRequired();
        if (labelRequired && dto.labelStart() == null) {
            throw new EquipmentItemException(EquipmentItemErrorCode.LABEL_START_REQUIRED);
        }
        if (!labelRequired && dto.labelStart() != null) {
            throw new EquipmentItemException(EquipmentItemErrorCode.LABEL_START_NOT_ALLOWED);
        }

        // 3. (라벨 사용) 접두사 행 잠금
        LabelPrefix prefix = null;
        if (labelRequired) {
            prefix = labelPrefixRepository.findWithLockById(model.getLabelPrefix().getId())
                    .orElseThrow(() -> new LabelPrefixException(LabelPrefixErrorCode.LABEL_PREFIX_NOT_FOUND));
        }

        // 4. 라벨 코드 목록 생성
        List<String> labelCodes = new ArrayList<>();
        if (labelRequired) {
            for (int i = 0; i < dto.quantity(); i++) {
                int number = dto.labelStart() + i;
                labelCodes.add(String.format("%s-%03d", prefix.getCode(), number));
            }
        }

        // 5. 라벨 중복 검사 (폐기 포함)
        if (labelRequired && equipmentItemRepository.existsByLabelCodeIn(labelCodes)) {
            throw new EquipmentItemException(EquipmentItemErrorCode.DUPLICATE_LABEL);
        }

        // 6. 기기 생성 후 saveAll
        List<EquipmentItem> items = new ArrayList<>();
        for (int i = 0; i < dto.quantity(); i++) {
            EquipmentItem.EquipmentItemBuilder builder = EquipmentItem.builder()
                    .equipmentModel(model)
                    .status(dto.status())
                    .memo(dto.memo());
            if (labelRequired) {
                builder.labelNumber(dto.labelStart() + i)
                        .labelCode(labelCodes.get(i));
            }
            items.add(builder.build());
        }
        // 5번 검사를 빠져나간 경우는 DB UNIQUE에서 막히므로 여기서 409로 바꾼다
        try {
            equipmentItemRepository.saveAllAndFlush(items);
        } catch (DataIntegrityViolationException exception) {
            throw new EquipmentItemException(EquipmentItemErrorCode.DUPLICATE_LABEL);
        }

        // 7. (라벨 사용) lastNumber 갱신
        if (labelRequired) {
            prefix.raiseLastNumberTo(dto.labelStart() + dto.quantity() - 1);
        }

        String firstLabelCode = labelRequired ? labelCodes.get(0) : null;
        String lastLabelCode = labelRequired ? labelCodes.get(labelCodes.size() - 1) : null;
        return new EquipmentItemCreateResponseDTO(model.getId(), dto.quantity(), firstLabelCode, lastLabelCode);
    }
}
