package com.mulmi.backend.domain.equipment.service;

import com.mulmi.backend.domain.equipment.dto.request.EquipmentItemCreateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.request.EquipmentItemUpdateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentItemCreateResponseDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentItemUpdateResponseDTO;
import com.mulmi.backend.domain.equipment.entity.EquipmentItem;
import com.mulmi.backend.domain.equipment.entity.EquipmentModel;
import com.mulmi.backend.domain.equipment.entity.LabelPrefix;
import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;
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
        // 0. 대여 예정·대여 중은 대여 흐름에서만 만들어지므로 등록 시 지정 불가
        validateManualStatus(dto.status());

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
                labelCodes.add(toLabelCode(prefix, number));
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

    // 기기 정보 수정 : 라벨을 바꿀 때만 접두사를 잠근다 (격리 수준 이유는 일괄 등록과 같음)
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public EquipmentItemUpdateResponseDTO updateEquipmentItem(Long itemId, EquipmentItemUpdateRequestDTO dto) {
        // 1. 상태 검사 + 폐기되지 않은 기기 조회
        validateManualStatus(dto.status());
        EquipmentItem item = equipmentItemRepository.findByIdAndDeletedAtIsNull(itemId)
                .orElseThrow(() -> new EquipmentItemException(EquipmentItemErrorCode.ITEM_NOT_FOUND));

        // 2. 라벨 번호가 바뀔 때만 : 같은 번호면 자기 자신과 겹쳐 409가 나므로 건너뛴다
        Integer newNumber = dto.labelNumber();
        if (newNumber != null && !newNumber.equals(item.getLabelNumber())) {
            EquipmentModel model = item.getEquipmentModel();
            if (!model.getCategory().isLabelRequired()) {
                throw new EquipmentItemException(EquipmentItemErrorCode.LABEL_NUMBER_NOT_ALLOWED);
            }
            LabelPrefix prefix = labelPrefixRepository.findWithLockById(model.getLabelPrefix().getId())
                    .orElseThrow(() -> new LabelPrefixException(LabelPrefixErrorCode.LABEL_PREFIX_NOT_FOUND));

            String newCode = toLabelCode(prefix, newNumber);
            if (equipmentItemRepository.existsByLabelCodeIn(List.of(newCode))) {
                throw new EquipmentItemException(EquipmentItemErrorCode.DUPLICATE_LABEL);
            }
            item.changeLabel(newNumber, newCode);
            prefix.raiseLastNumberTo(newNumber);
        }

        // 3. 상태·메모 변경 (메모는 null이면 유지)
        item.changeStatus(dto.status());
        if (dto.memo() != null) {
            item.changeMemo(dto.memo());
        }

        // 4. 검사를 빠져나간 라벨 중복은 DB UNIQUE에서 막히므로 409로 바꾼다
        try {
            equipmentItemRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new EquipmentItemException(EquipmentItemErrorCode.DUPLICATE_LABEL);
        }

        return new EquipmentItemUpdateResponseDTO(
                item.getId(), item.getLabelCode(), item.getStatus(), item.getMemo());
    }

    // 기기 폐기 : 이미 폐기된 기기를 409로 구분하려고 삭제 여부와 상관없이 조회한다
    @Override
    @Transactional
    public void disposeEquipmentItem(Long itemId) {
        EquipmentItem item = equipmentItemRepository.findById(itemId)
                .orElseThrow(() -> new EquipmentItemException(EquipmentItemErrorCode.ITEM_NOT_FOUND));

        if (item.getDeletedAt() != null) {
            throw new EquipmentItemException(EquipmentItemErrorCode.ITEM_ALREADY_DISPOSED);
        }
        if (item.getStatus() == EquipmentItemStatus.RESERVED || item.getStatus() == EquipmentItemStatus.RENTED) {
            throw new EquipmentItemException(EquipmentItemErrorCode.ITEM_IN_USE);
        }

        // 라벨 번호와 lastNumber는 그대로 둔다 (폐기된 라벨은 재사용하지 않음)
        item.dispose();
    }

    // 라벨 코드 형식은 이 메서드 하나에서만 만든다 (검사용·저장용이 어긋나지 않게)
    private String toLabelCode(LabelPrefix prefix, int number) {
        return String.format("%s-%03d", prefix.getCode(), number);
    }

    // 조교가 직접 지정할 수 있는 상태는 대여 가능·고장뿐 (등록·수정 공통)
    private void validateManualStatus(EquipmentItemStatus status) {
        if (status != EquipmentItemStatus.AVAILABLE && status != EquipmentItemStatus.BROKEN) {
            throw new EquipmentItemException(EquipmentItemErrorCode.INVALID_ITEM_STATUS);
        }
    }
}
