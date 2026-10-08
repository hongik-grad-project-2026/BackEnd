package com.mulmi.backend.domain.equipment.service;

import com.mulmi.backend.domain.equipment.dto.request.EquipmentModelCreateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentModelCreateResponseDTO;
import com.mulmi.backend.domain.equipment.repository.CategoryRepository;
import com.mulmi.backend.domain.equipment.repository.EquipmentModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentModelServiceImpl implements EquipmentModelService {

    private final EquipmentModelRepository equipmentModelRepository;
    private final CategoryRepository categoryRepository;

    // 모델 등록
    @Override
    @Transactional
    public EquipmentModelCreateResponseDTO createEquipmentModel(EquipmentModelCreateRequestDTO dto) {
        return null;
    }
}
