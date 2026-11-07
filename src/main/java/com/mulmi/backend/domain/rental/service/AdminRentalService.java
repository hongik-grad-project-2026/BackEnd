package com.mulmi.backend.domain.rental.service;

import com.mulmi.backend.domain.rental.dto.request.RentalRejectRequestDTO;
import com.mulmi.backend.domain.rental.dto.response.AdminRentalDetailResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.AdminRentalPageResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalCheckoutResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalImageListResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalImageUploadResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalReturnResponseDTO;
import com.mulmi.backend.domain.rental.enums.RentalImageType;
import com.mulmi.backend.domain.rental.enums.RentalStatus;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import com.mulmi.backend.domain.rental.dto.response.RentalApproveResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalRejectResponseDTO;

public interface AdminRentalService {

    // 관리자 대여 목록 (명세 50번)
    AdminRentalPageResponseDTO getAdminRentals(List<RentalStatus> statuses, String keyword, int page, int size);

    // 관리자 대여 상세 (명세 51번)
    AdminRentalDetailResponseDTO getAdminRental(Long rentalId);

    // 수령 기한 처리 (명세 58번, 조교 API 없이 스케줄러만) : 후보 찾기 + 한 건씩 처리
    //  후보 = 이 상태이면서 수령 기한이 지난 대여 (수령 대기 → 노쇼, 승인 대기 → 자동 취소)
    List<Long> findPickupExpiredIds(RentalStatus status, LocalDateTime now);

    boolean markNoShow(Long rentalId, LocalDateTime now);

    boolean cancelExpiredApplication(Long rentalId, LocalDateTime now);

    // 사진 조회 (명세 56번) : imageType이 null이면 전체
    RentalImageListResponseDTO getRentalImages(Long rentalId, RentalImageType imageType);

    // 대여 승인 (명세 52번)
    RentalApproveResponseDTO approveRental(Long rentalId, Long assistantId);

    // 수령 처리 (명세 54번)
    RentalCheckoutResponseDTO checkoutRental(Long rentalId, Long staffId);

    // 반납 처리 (명세 57번)
    RentalReturnResponseDTO returnRental(Long rentalId, Long staffId);

    // 대여 사진 업로드 (명세 55번)
    RentalImageUploadResponseDTO uploadRentalImage(
            Long rentalId, Long uploaderId, RentalImageType imageType, MultipartFile image);

    // 대여 반려 (명세 53번)
    RentalRejectResponseDTO rejectRental(Long rentalId, Long assistantId, RentalRejectRequestDTO dto);
}
