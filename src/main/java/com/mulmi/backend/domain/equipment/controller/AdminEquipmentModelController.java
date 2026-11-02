package com.mulmi.backend.domain.equipment.controller;

import com.mulmi.backend.domain.equipment.dto.request.EquipmentModelCreateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.response.AdminEquipmentItemPageResponseDTO;
import com.mulmi.backend.domain.equipment.dto.response.AdminEquipmentModelDetailResponseDTO;
import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;
import com.mulmi.backend.domain.equipment.dto.response.AdminEquipmentModelPageResponseDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentModelCreateResponseDTO;
import com.mulmi.backend.domain.equipment.dto.response.EquipmentModelImageUploadResponseDTO;
import com.mulmi.backend.domain.equipment.exception.code.EquipmentModelSuccessCode;
import com.mulmi.backend.domain.equipment.service.EquipmentModelService;
import com.mulmi.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/admin/equipment-models")
@Tag(name = "모델 관리", description = "조교용 모델 관리 API")
public class AdminEquipmentModelController {

    private final EquipmentModelService equipmentModelService;

    // 모델 등록
    @Operation(summary = "모델 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ASSISTANT')")
    public ApiResponse<EquipmentModelCreateResponseDTO> createEquipmentModel(
            @RequestBody @Valid EquipmentModelCreateRequestDTO dto
    ) {
        return ApiResponse.onSuccess(
                EquipmentModelSuccessCode.MODEL_CREATED,
                equipmentModelService.createEquipmentModel(dto)
        );
    }

    // 모델 이미지 업로드
    @Operation(summary = "모델 이미지 업로드")
    @PatchMapping(value = "/{model_id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ASSISTANT')")
    public ApiResponse<EquipmentModelImageUploadResponseDTO> uploadModelImage(
            @PathVariable("model_id") Long modelId,
            @RequestPart("image") MultipartFile image) {
        return ApiResponse.onSuccess(
                EquipmentModelSuccessCode.IMAGE_UPLOADED,
                equipmentModelService.uploadModelImage(modelId, image)
        );
    }

    // 관리자 모델 목록 조회
    @Operation(summary = "기자재 모델 목록")
    @GetMapping
    @PreAuthorize("hasAnyRole('ASSISTANT', 'WORKER', 'STUDENT')")
    public ApiResponse<AdminEquipmentModelPageResponseDTO> getModelPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.onSuccess(
                EquipmentModelSuccessCode.MODELS_FOUND,
                equipmentModelService.getAdminEquipmentModels(categoryId, keyword, page, size)
        );
    }

    // 관리자 모델 단건 조회 (기기 목록 페이지 머리말)
    @Operation(summary = "기자재 모델 단건 조회")
    @GetMapping("/{model_id}")
    @PreAuthorize("hasAnyRole('ASSISTANT', 'WORKER', 'STUDENT')")
    public ApiResponse<AdminEquipmentModelDetailResponseDTO> getModel(
            @PathVariable("model_id") Long modelId
    ) {
        return ApiResponse.onSuccess(
                EquipmentModelSuccessCode.MODEL_FOUND,
                equipmentModelService.getAdminEquipmentModel(modelId)
        );
    }

    // 모델별 기기 목록 조회
    @Operation(summary = "모델별 기기 목록 조회")
    @GetMapping("/{model_id}/items")
    @PreAuthorize("hasAnyRole('ASSISTANT', 'WORKER')")
    public ApiResponse<AdminEquipmentItemPageResponseDTO> getItemPage(
            @PathVariable("model_id") Long modelId,
            @RequestParam(required = false) EquipmentItemStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.onSuccess(
                EquipmentModelSuccessCode.ITEMS_FOUND,
                equipmentModelService.getAdminEquipmentItems(modelId, status, keyword, page, size)
        );
    }
}
