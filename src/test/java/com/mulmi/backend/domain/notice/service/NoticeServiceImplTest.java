package com.mulmi.backend.domain.notice.service;

import com.mulmi.backend.domain.notice.dto.request.NoticeCreateRequestDTO;
import com.mulmi.backend.domain.notice.dto.request.NoticeUpdateRequestDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticeDetailResponseDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticeAttachmentResponseDTO;
import com.mulmi.backend.domain.notice.dto.response.NoticePageResponseDTO;
import com.mulmi.backend.domain.notice.entity.Notice;
import com.mulmi.backend.domain.notice.entity.NoticeAttachment;
import com.mulmi.backend.domain.notice.repository.NoticeRepository;
import com.mulmi.backend.domain.notice.repository.NoticeAttachmentRepository;
import com.mulmi.backend.domain.notice.exception.NoticeException;
import com.mulmi.backend.domain.notice.exception.code.NoticeErrorCode;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.domain.user.enums.UserRole;
import com.mulmi.backend.domain.user.enums.UserStatus;
import com.mulmi.backend.domain.user.exception.UserException;
import com.mulmi.backend.domain.user.exception.code.UserErrorCode;
import com.mulmi.backend.domain.user.repository.UserRepository;
import com.mulmi.backend.global.storage.S3StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NoticeServiceImplTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private NoticeAttachmentRepository noticeAttachmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3StorageService s3StorageService;

    @InjectMocks
    private NoticeServiceImpl noticeService;

    @Test
    void getNoticesReturnsNoticePage() {
        User author = createAuthor();
        Notice notice = Notice.builder()
                .id(1L)
                .title("대여실 운영 안내")
                .content("운영 시간을 안내합니다.")
                .important(true)
                .author(author)
                .build();
        PageRequest pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );
        given(noticeRepository.findNotices(null, pageable))
                .willReturn(new PageImpl<>(List.of(notice), pageable, 1));

        NoticePageResponseDTO result = noticeService.getNotices(null, 0, 20);

        assertThat(result.notices()).hasSize(1);
        assertThat(result.notices().get(0).noticeId()).isEqualTo(1L);
        assertThat(result.notices().get(0).title()).isEqualTo("대여실 운영 안내");
        assertThat(result.notices().get(0).important()).isTrue();
        assertThat(result.notices().get(0).authorId()).isEqualTo(2L);
        assertThat(result.notices().get(0).authorName()).isEqualTo("조교");
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void getNoticesFiltersRecentImportantNoticesForDashboard() {
        PageRequest pageable = PageRequest.of(
                0,
                3,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );
        given(noticeRepository.findNotices(true, pageable))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        NoticePageResponseDTO result = noticeService.getNotices(true, 0, 3);

        assertThat(result.notices()).isEmpty();
        assertThat(result.size()).isEqualTo(3);
        verify(noticeRepository).findNotices(true, pageable);
    }

    @Test
    void getNoticeReturnsNoticeDetail() {
        User author = createAuthor();
        Notice notice = Notice.builder()
                .id(1L)
                .title("대여실 운영 안내")
                .content("운영 시간을 안내합니다.")
                .important(true)
                .author(author)
                .build();
        given(noticeRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(notice));

        NoticeDetailResponseDTO result = noticeService.getNotice(1L);

        assertThat(result.noticeId()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("대여실 운영 안내");
        assertThat(result.content()).isEqualTo("운영 시간을 안내합니다.");
        assertThat(result.important()).isTrue();
        assertThat(result.authorId()).isEqualTo(2L);
        assertThat(result.authorName()).isEqualTo("조교");
    }

    @Test
    void getNoticeThrowsWhenNoticeDoesNotExist() {
        given(noticeRepository.findByIdAndDeletedAtIsNull(999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> noticeService.getNotice(999L))
                .isInstanceOf(NoticeException.class)
                .extracting("code")
                .isEqualTo(NoticeErrorCode.NOTICE_NOT_FOUND);
    }

    @Test
    void createNoticeSavesNoticeWithAuthenticatedAuthor() {
        User author = createAuthor();
        NoticeCreateRequestDTO request = new NoticeCreateRequestDTO(
                " 대여실 운영 안내 ",
                " 운영 시간을 안내합니다. ",
                true
        );
        given(userRepository.findById(2L)).willReturn(Optional.of(author));
        given(noticeRepository.save(any(Notice.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        NoticeDetailResponseDTO result = noticeService.createNotice(2L, request);

        assertThat(result.title()).isEqualTo("대여실 운영 안내");
        assertThat(result.content()).isEqualTo("운영 시간을 안내합니다.");
        assertThat(result.important()).isTrue();
        assertThat(result.authorId()).isEqualTo(2L);
        verify(noticeRepository).save(any(Notice.class));
    }

    @Test
    void createNoticeThrowsWhenAuthorIsInactive() {
        User inactiveAuthor = User.builder()
                .id(2L)
                .loginId("assistant")
                .password("encoded-password")
                .name("조교")
                .email("assistant@example.com")
                .phoneNumber("01012345678")
                .role(UserRole.ASSISTANT)
                .status(UserStatus.SUSPENDED)
                .build();
        NoticeCreateRequestDTO request = new NoticeCreateRequestDTO(
                "공지사항",
                "공지사항 내용",
                false
        );
        given(userRepository.findById(2L)).willReturn(Optional.of(inactiveAuthor));

        assertThatThrownBy(() -> noticeService.createNotice(2L, request))
                .isInstanceOf(UserException.class)
                .extracting("code")
                .isEqualTo(UserErrorCode.INACTIVE_USER);
    }

    @Test
    void updateNoticeUpdatesExistingNotice() {
        User author = createAuthor();
        Notice notice = Notice.builder()
                .id(1L)
                .title("기존 제목")
                .content("기존 내용")
                .important(false)
                .author(author)
                .build();
        NoticeUpdateRequestDTO request = new NoticeUpdateRequestDTO(
                " 수정 제목 ",
                " 수정 내용 ",
                true
        );
        given(noticeRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(notice));

        NoticeDetailResponseDTO result = noticeService.updateNotice(1L, request);

        assertThat(result.title()).isEqualTo("수정 제목");
        assertThat(result.content()).isEqualTo("수정 내용");
        assertThat(result.important()).isTrue();
        assertThat(result.authorId()).isEqualTo(2L);
    }

    @Test
    void updateNoticeThrowsWhenNoticeDoesNotExist() {
        NoticeUpdateRequestDTO request = new NoticeUpdateRequestDTO(
                "수정 제목",
                "수정 내용",
                false
        );
        given(noticeRepository.findByIdAndDeletedAtIsNull(999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> noticeService.updateNotice(999L, request))
                .isInstanceOf(NoticeException.class)
                .extracting("code")
                .isEqualTo(NoticeErrorCode.NOTICE_NOT_FOUND);
    }

    @Test
    void deleteNoticeSoftDeletesExistingNotice() {
        Notice notice = Notice.builder()
                .id(1L)
                .title("삭제할 공지")
                .content("공지 내용")
                .important(false)
                .author(createAuthor())
                .build();
        given(noticeRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(notice));

        noticeService.deleteNotice(1L);

        assertThat(notice.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteNoticeThrowsWhenNoticeDoesNotExist() {
        given(noticeRepository.findByIdAndDeletedAtIsNull(999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> noticeService.deleteNotice(999L))
                .isInstanceOf(NoticeException.class)
                .extracting("code")
                .isEqualTo(NoticeErrorCode.NOTICE_NOT_FOUND);
    }

    @Test
    void uploadAttachmentStoresFileAndMetadata() {
        Notice notice = Notice.builder()
                .id(1L)
                .title("첨부 공지")
                .content("첨부파일이 있습니다.")
                .important(false)
                .author(createAuthor())
                .build();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "대여안내.pdf",
                "application/pdf",
                "pdf-content".getBytes()
        );
        given(noticeRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(notice));
        given(s3StorageService.upload(file, "notices/1/attachments"))
                .willReturn("notices/1/attachments/generated.pdf");
        given(noticeAttachmentRepository.save(any(NoticeAttachment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        NoticeAttachmentResponseDTO result = noticeService.uploadAttachment(1L, file);

        assertThat(result.originalFileName()).isEqualTo("대여안내.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.fileSize()).isEqualTo(file.getSize());
        verify(noticeAttachmentRepository).save(any(NoticeAttachment.class));
    }

    @Test
    void uploadAttachmentRejectsUnsupportedFileType() {
        Notice notice = Notice.builder()
                .id(1L)
                .title("첨부 공지")
                .content("첨부파일이 있습니다.")
                .important(false)
                .author(createAuthor())
                .build();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.exe",
                "application/octet-stream",
                "executable".getBytes()
        );
        given(noticeRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(notice));

        assertThatThrownBy(() -> noticeService.uploadAttachment(1L, file))
                .isInstanceOf(NoticeException.class)
                .extracting("code")
                .isEqualTo(NoticeErrorCode.INVALID_ATTACHMENT);
    }

    @Test
    void createAttachmentDownloadUrlReturnsPresignedUrl() {
        Notice notice = Notice.builder()
                .id(1L)
                .title("첨부 공지")
                .content("첨부파일이 있습니다.")
                .important(false)
                .author(createAuthor())
                .build();
        NoticeAttachment attachment = NoticeAttachment.builder()
                .id(10L)
                .notice(notice)
                .originalFileName("대여안내.pdf")
                .storedFileName("generated.pdf")
                .fileUrl("notices/1/attachments/generated.pdf")
                .contentType("application/pdf")
                .fileSize(100L)
                .build();
        given(noticeRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(notice));
        given(noticeAttachmentRepository.findByIdAndNoticeId(10L, 1L))
                .willReturn(Optional.of(attachment));
        given(s3StorageService.createDownloadUrl(
                "notices/1/attachments/generated.pdf",
                "대여안내.pdf",
                "application/pdf"
        )).willReturn("https://example.com/download");

        String result = noticeService.createAttachmentDownloadUrl(1L, 10L);

        assertThat(result).isEqualTo("https://example.com/download");
    }

    @Test
    void createAttachmentDownloadUrlRejectsAttachmentFromAnotherNotice() {
        Notice notice = Notice.builder()
                .id(1L)
                .title("첨부 공지")
                .content("첨부파일이 있습니다.")
                .important(false)
                .author(createAuthor())
                .build();
        given(noticeRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(notice));
        given(noticeAttachmentRepository.findByIdAndNoticeId(10L, 1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> noticeService.createAttachmentDownloadUrl(1L, 10L))
                .isInstanceOf(NoticeException.class)
                .extracting("code")
                .isEqualTo(NoticeErrorCode.ATTACHMENT_NOT_FOUND);
    }

    private User createAuthor() {
        return User.builder()
                .id(2L)
                .loginId("assistant")
                .password("encoded-password")
                .name("조교")
                .email("assistant@example.com")
                .phoneNumber("01012345678")
                .role(UserRole.ASSISTANT)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
