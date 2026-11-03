package com.mulmi.backend.domain.equipment.controller;

import com.mulmi.backend.domain.equipment.dto.request.EquipmentItemCreateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentItemCreateResponseDTO;
import com.mulmi.backend.domain.equipment.exception.code.EquipmentItemSuccessCode;
import com.mulmi.backend.domain.equipment.service.EquipmentItemService;
import com.mulmi.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/equipment-items")
@Tag(name = "기기 관리", description = "조교용 개별 기기 관리 API")
public class AdminEquipmentItemController {

    private final EquipmentItemService equipmentItemService;

    // 기기 일괄 등록
    @Operation(summary = "기기 일괄 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ASSISTANT')")
    public ApiResponse<EquipmentItemCreateResponseDTO> createEquipmentItems(
            @RequestBody @Valid EquipmentItemCreateRequestDTO dto
    ) {
        return ApiResponse.onSuccess(
                EquipmentItemSuccessCode.ITEMS_CREATED,
                equipmentItemService.createEquipmentItems(dto)
        );
    }
}
