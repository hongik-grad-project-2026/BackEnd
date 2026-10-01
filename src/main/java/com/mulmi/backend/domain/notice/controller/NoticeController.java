package com.mulmi.backend.domain.notice.controller;

import com.mulmi.backend.domain.notice.dto.request.NoticeCreateRequestDTO;
import com.mulmi.backend.domain.notice.dto.request.NoticeUpdateRequestDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticeDetailResponseDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticeAttachmentResponseDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticePageResponseDTO;
import com.mulmi.backend.domain.notice.exception.code.NoticeSuccessCode;
import com.mulmi.backend.domain.notice.service.NoticeService;
import com.mulmi.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
@Tag(name = "공지사항", description = "공지사항 조회 및 관리 API")
public class NoticeController {

    private final NoticeService noticeService;

    // 공지사항 목록 조회
    @Operation(summary = "공지사항 목록 조회")
    @GetMapping
    public ApiResponse<NoticePageResponseDTO> getNotices(
            @RequestParam(required = false) Boolean important,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.onSuccess(
                NoticeSuccessCode.NOTICES_FOUND,
                noticeService.getNotices(important, page, size)
        );
    }

    // 공지사항 상세 조회
    @Operation(summary = "공지사항 상세 조회")
    @GetMapping("/{noticeId}")
    public ApiResponse<NoticeDetailResponseDTO> getNotice(
            @PathVariable Long noticeId
    ) {
        return ApiResponse.onSuccess(
                NoticeSuccessCode.NOTICE_FOUND,
                noticeService.getNotice(noticeId)
        );
    }

    // 공지사항 작성
    @Operation(summary = "공지사항 작성")
    @PostMapping
    @PreAuthorize("hasAnyRole('ASSISTANT', 'WORKER')")
    public ApiResponse<NoticeDetailResponseDTO> createNotice(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid NoticeCreateRequestDTO dto
    ) {
        return ApiResponse.onSuccess(
                NoticeSuccessCode.NOTICE_CREATED,
                noticeService.createNotice(userId, dto)
        );
    }

    // 공지사항 수정
    @Operation(summary = "공지사항 수정")
    @PatchMapping("/{noticeId}")
    @PreAuthorize("hasAnyRole('ASSISTANT', 'WORKER')")
    public ApiResponse<NoticeDetailResponseDTO> updateNotice(
            @PathVariable Long noticeId,
            @RequestBody @Valid NoticeUpdateRequestDTO dto
    ) {
        return ApiResponse.onSuccess(
                NoticeSuccessCode.NOTICE_UPDATED,
                noticeService.updateNotice(noticeId, dto)
        );
    }

    // 공지사항 삭제
    @Operation(summary = "공지사항 삭제")
    @DeleteMapping("/{noticeId}")
    @PreAuthorize("hasAnyRole('ASSISTANT', 'WORKER')")
    public ApiResponse<Void> deleteNotice(
            @PathVariable Long noticeId
    ) {
        noticeService.deleteNotice(noticeId);
        return ApiResponse.onSuccess(NoticeSuccessCode.NOTICE_DELETED, null);
    }

    // 공지사항 첨부파일 업로드
    @Operation(summary = "공지사항 첨부파일 업로드")
    @PostMapping(value = "/{noticeId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ASSISTANT', 'WORKER')")
    public ApiResponse<NoticeAttachmentResponseDTO> uploadAttachment(
            @PathVariable Long noticeId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.onSuccess(
                NoticeSuccessCode.ATTACHMENT_UPLOADED,
                noticeService.uploadAttachment(noticeId, file)
        );
    }

    // 공지사항 첨부파일 다운로드
    @Operation(summary = "공지사항 첨부파일 다운로드")
    @GetMapping("/{noticeId}/attachments/{attachmentId}/download")
    public ResponseEntity<Void> downloadAttachment(
            @PathVariable Long noticeId,
            @PathVariable Long attachmentId
    ) {
        String downloadUrl = noticeService.createAttachmentDownloadUrl(
                noticeId,
                attachmentId
        );
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(downloadUrl))
                .build();
    }
}
