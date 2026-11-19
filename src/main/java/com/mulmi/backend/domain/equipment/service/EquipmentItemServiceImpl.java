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
import com.mulmi.backend.domain.equipment.repository.CategoryRepository;
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
    private final CategoryRepository categoryRepository;

    // READ_COMMITTED : MySQL 기본(REPEATABLE READ)이면 잠금을 기다린 뒤의 중복 검사가
    // 트랜잭션 첫 조회 시점 스냅샷을 읽어 앞 요청이 커밋한 라벨을 보지 못한다
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public EquipmentItemCreateResponseDTO createEquipmentItems(EquipmentItemCreateRequestDTO dto) {
        // 0. 대여 예정·대여 중은 대여 흐름에서만 만들어지므로 등록 시 지정 불가
        validateManualStatus(dto.status());

        // 1. 폐기되지 않은 모델을 잠그며 조회 : 모델 수정(카테고리·접두사 변경)·모델 폐기와 한 줄로 선다
        //    잠금이 첫 조회라 기다린 뒤 최신 커밋 값을 읽는다 → 폐기됐으면 404, 커밋까지 모델이 바뀌지 않는다
        EquipmentModel model = equipmentModelRepository.findWithLockByIdAndDeletedAtIsNull(dto.modelId())
                .orElseThrow(()-> new EquipmentModelException(EquipmentModelErrorCode.MODEL_NOT_FOUND));

        // 2. 시작 번호 : 접두사 카테고리는 필수, 기타는 없으면 서버가 발급 (11-19 프론트 요청)
        //    isLabelRequired()는 이제 "접두사를 쓰는가"를 뜻한다 (노트북·태블릿·보조배터리 = 접두사, 기타 = 번호만)
        boolean hasPrefix = model.getCategory().isLabelRequired();
        if (hasPrefix && dto.labelStart() == null) {
            throw new EquipmentItemException(EquipmentItemErrorCode.LABEL_START_REQUIRED);
        }

        // 3. 번호 공간 잠금 후 시작 번호 결정
        //    접두사 있음 : 같은 접두사를 쓰는 모델끼리(예: 맥북 여러 모델이 M) 한 줄로 → lastNumber 갱신
        //    접두사 없음 : 접두사 없는 번호(007)는 카테고리가 달라도 한 공간이라 그 카테고리 전부를 잠근다
        //                 시작 번호를 보내면 그 번호부터, 없으면 접두사 없는 번호 중 최대 + 1부터 (기다린 뒤 읽으므로 앞 요청 번호까지 보임)
        LabelPrefix prefix = null;
        long labelStart;
        if (hasPrefix) {
            prefix = labelPrefixRepository.findWithLockById(model.getLabelPrefix().getId())
                    .orElseThrow(() -> new LabelPrefixException(LabelPrefixErrorCode.LABEL_PREFIX_NOT_FOUND));
            labelStart = dto.labelStart();
        } else {
            categoryRepository.findAllWithLockByLabelRequiredFalseOrderByIdAsc();
            labelStart = dto.labelStart() != null
                    ? dto.labelStart()
                    : equipmentItemRepository.findMaxUnprefixedLabelNumber().orElse(0) + 1L;
        }

        // 3-1. 마지막 번호가 저장 가능한 범위(int)를 넘으면 거절 : long으로 계산해야 넘침을 알아챈다
        //      (int로 더하면 2147483647 + 1이 -2147483648로 바뀐다)
        long labelEnd = labelStart + dto.quantity() - 1;
        if (labelEnd > Integer.MAX_VALUE) {
            throw new EquipmentItemException(EquipmentItemErrorCode.LABEL_NUMBER_OUT_OF_RANGE);
        }

        // 4. 라벨 코드 목록 생성 (접두사 있으면 M-041, 없으면 041)
        List<String> labelCodes = new ArrayList<>();
        for (int i = 0; i < dto.quantity(); i++) {
            labelCodes.add(toLabelCode(prefix, (int) labelStart + i));
        }

        // 5. 라벨 중복 검사 (폐기 포함 → 폐기된 번호는 재사용 불가)
        if (equipmentItemRepository.existsByLabelCodeIn(labelCodes)) {
            throw new EquipmentItemException(EquipmentItemErrorCode.DUPLICATE_LABEL);
        }

        // 6. 기기 생성 후 saveAll
        List<EquipmentItem> items = new ArrayList<>();
        for (int i = 0; i < dto.quantity(); i++) {
            items.add(EquipmentItem.builder()
                    .equipmentModel(model)
                    .status(dto.status())
                    .memo(dto.memo())
                    .labelNumber((int) labelStart + i)
                    .labelCode(labelCodes.get(i))
                    .build());
        }
        // 5번 검사를 빠져나간 경우는 DB UNIQUE에서 막히므로 여기서 409로 바꾼다
        try {
            equipmentItemRepository.saveAllAndFlush(items);
        } catch (DataIntegrityViolationException exception) {
            throw new EquipmentItemException(EquipmentItemErrorCode.DUPLICATE_LABEL);
        }

        // 7. (접두사 사용) lastNumber 갱신
        if (hasPrefix) {
            prefix.raiseLastNumberTo((int) labelEnd);
        }

        return new EquipmentItemCreateResponseDTO(model.getId(), dto.quantity(),
                labelCodes.get(0), labelCodes.get(labelCodes.size() - 1));
    }

    // 기기 정보 수정 : 라벨을 바꿀 때만 접두사를 잠근다 (격리 수준 이유는 일괄 등록과 같음)
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public EquipmentItemUpdateResponseDTO updateEquipmentItem(Long itemId, EquipmentItemUpdateRequestDTO dto) {
        // 1. 상태 검사 + 폐기되지 않은 기기를 잠그며 조회
        validateManualStatus(dto.status());
        EquipmentItem item = equipmentItemRepository.findWithLockByIdAndDeletedAtIsNull(itemId)
                .orElseThrow(() -> new EquipmentItemException(EquipmentItemErrorCode.ITEM_NOT_FOUND));

        // 1-1. 대여 흐름 중(예약·대여 중)인 기기는 수정 불가 (11-07 결정)
        //      중간에 고장 나도 반려·반납으로 대여 기록을 끝낸 뒤 고장으로 바꾼다
        if (EquipmentItemStatus.IN_USE.contains(item.getStatus())) {
            throw new EquipmentItemException(EquipmentItemErrorCode.ITEM_IN_RENTAL);
        }

        // 2. 라벨 번호가 바뀔 때만 : 같은 번호면 자기 자신과 겹쳐 409가 나므로 건너뛴다
        Integer newNumber = dto.labelNumber();
        //    기타도 번호 수정 가능 (11-19 결정). 접두사 잠금·lastNumber 갱신은 접두사가 있을 때만
        if (newNumber != null && !newNumber.equals(item.getLabelNumber())) {
            EquipmentModel model = item.getEquipmentModel();
            LabelPrefix prefix = null;
            if (model.getCategory().isLabelRequired()) {
                prefix = labelPrefixRepository.findWithLockById(model.getLabelPrefix().getId())
                        .orElseThrow(() -> new LabelPrefixException(LabelPrefixErrorCode.LABEL_PREFIX_NOT_FOUND));
            } else {
                // 접두사 없음 : 자동 발급과 같은 잠금을 잡아, 발급이 이 수정 번호를 보고 다음 번호를 고르게 한다
                categoryRepository.findAllWithLockByLabelRequiredFalseOrderByIdAsc();
            }

            String newCode = toLabelCode(prefix, newNumber);
            if (equipmentItemRepository.existsByLabelCodeIn(List.of(newCode))) {
                throw new EquipmentItemException(EquipmentItemErrorCode.DUPLICATE_LABEL);
            }
            item.changeLabel(newNumber, newCode);
            if (prefix != null) {
                prefix.raiseLastNumberTo(newNumber);
            }
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
    //  기기 행을 잠그고 나서 상태를 본다 — 잠금 없이 읽으면 그 사이 대여 신청이 RESERVED로 바꿔도
    //  옛 상태(AVAILABLE)로 검사를 통과해, 진행 중인 대여에 폐기된 기기가 연결된다
    //  첫 쿼리가 잠금이라 기본 격리 수준으로 충분하다 (잠그며 읽으면 최신 커밋 값을 본다)
    @Override
    @Transactional
    public void disposeEquipmentItem(Long itemId) {
        EquipmentItem item = equipmentItemRepository.findWithLockById(itemId)
                .orElseThrow(() -> new EquipmentItemException(EquipmentItemErrorCode.ITEM_NOT_FOUND));

        if (item.getDeletedAt() != null) {
            throw new EquipmentItemException(EquipmentItemErrorCode.ITEM_ALREADY_DISPOSED);
        }
        if (EquipmentItemStatus.IN_USE.contains(item.getStatus())) {
            throw new EquipmentItemException(EquipmentItemErrorCode.ITEM_IN_USE);
        }

        // 라벨 번호와 lastNumber는 그대로 둔다 (폐기된 라벨은 재사용하지 않음)
        item.dispose();
    }

    // 라벨 코드 형식은 이 메서드 하나에서만 만든다 (검사용·저장용이 어긋나지 않게)
    // 접두사 없음(기타) → "007" : "M-007"과 절대 겹치지 않으므로 label_code UNIQUE가 곧 기타 안 중복 금지가 된다
    private String toLabelCode(LabelPrefix prefix, int number) {
        if (prefix == null) {
            return String.format("%03d", number);
        }
        return String.format("%s-%03d", prefix.getCode(), number);
    }

    // 조교가 직접 지정할 수 있는 상태는 대여 가능·고장뿐 (등록·수정 공통)
    private void validateManualStatus(EquipmentItemStatus status) {
        if (status != EquipmentItemStatus.AVAILABLE && status != EquipmentItemStatus.BROKEN) {
            throw new EquipmentItemException(EquipmentItemErrorCode.INVALID_ITEM_STATUS);
        }
    }
}
