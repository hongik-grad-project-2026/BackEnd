package com.mulmi.backend.domain.notice.controller;

import com.mulmi.backend.domain.notice.dto.response.NoticeAttachmentDownloadResponseDTO;
import com.mulmi.backend.domain.notice.service.NoticeService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class NoticeControllerTest {

    private final NoticeService noticeService = mock(NoticeService.class);
    private final NoticeController noticeController = new NoticeController(noticeService);

    @Test
    void downloadAttachmentReturnsFileWithoutS3Redirect() {
        byte[] content = "pdf-content".getBytes();
        given(noticeService.downloadAttachment(1L, 10L))
                .willReturn(new NoticeAttachmentDownloadResponseDTO(
                        "대여안내.pdf",
                        MediaType.APPLICATION_PDF_VALUE,
                        content
                ));

        var response = noticeController.downloadAttachment(1L, 10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(content.length);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment")
                .contains("UTF-8");
        assertThat(response.getBody()).isEqualTo(content);
    }
}
