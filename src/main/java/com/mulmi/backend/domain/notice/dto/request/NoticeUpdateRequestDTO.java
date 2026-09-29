package com.mulmi.backend.domain.notice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoticeUpdateRequestDTO(
        @NotBlank(message = "공지사항 제목은 필수입니다.")
        @Size(max = 200, message = "공지사항 제목은 200자를 넘을 수 없습니다.")
        String title,

        @NotBlank(message = "공지사항 내용은 필수입니다.")
        String content,

        @NotNull(message = "중요 공지 여부는 필수입니다.")
        Boolean important
) {
}
