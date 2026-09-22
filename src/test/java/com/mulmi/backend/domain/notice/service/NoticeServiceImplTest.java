package com.mulmi.backend.domain.notice.service;

import com.mulmi.backend.domain.notice.dto.response.NoticePageResponseDTO;
import com.mulmi.backend.domain.notice.entity.Notice;
import com.mulmi.backend.domain.notice.repository.NoticeRepository;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.domain.user.enums.UserRole;
import com.mulmi.backend.domain.user.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NoticeServiceImplTest {

    @Mock
    private NoticeRepository noticeRepository;

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
