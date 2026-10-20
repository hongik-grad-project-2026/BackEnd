package com.mulmi.backend.domain.equipment.controller;

import com.mulmi.backend.domain.equipment.dto.request.EquipmentModelCreateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentModelCreateResponseDTO;
import com.mulmi.backend.domain.equipment.exception.code.EquipmentModelSuccessCode;
import com.mulmi.backend.domain.equipment.service.EquipmentModelService;
import com.mulmi.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/equipment-models")
@Tag(name = "모델 관리", description = "조교용 모델 관리 API")
public class AdminEquipmentModelController {

    private final EquipmentModelService equipmentModelService;

    // 모델 등록
    @Operation(summary = "모델 등록")
    @PostMapping
    @PreAuthorize("hasRole('ASSISTANT')")
    public ApiResponse<EquipmentModelCreateResponseDTO> createEquipmentModel(
            @RequestBody @Valid EquipmentModelCreateRequestDTO dto
    ) {
        return ApiResponse.onSuccess(
                EquipmentModelSuccessCode.MODEL_CREATED,
                equipmentModelService.createEquipmentModel(dto)
        );
    }
}
