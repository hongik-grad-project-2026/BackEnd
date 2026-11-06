package com.mulmi.backend.domain.rental.service;

import com.mulmi.backend.domain.rental.dto.request.RentalRejectRequestDTO;
import com.mulmi.backend.domain.rental.dto.response.AdminRentalPageResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalImageUploadResponseDTO;
import com.mulmi.backend.domain.rental.enums.RentalImageType;
import com.mulmi.backend.domain.rental.enums.RentalStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import com.mulmi.backend.domain.rental.dto.response.RentalApproveResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalRejectResponseDTO;

public interface AdminRentalService {

    // 관리자 대여 목록 (명세 50번)
    AdminRentalPageResponseDTO getAdminRentals(List<RentalStatus> statuses, String keyword, int page, int size);

    // 대여 승인 (명세 52번)
    RentalApproveResponseDTO approveRental(Long rentalId, Long assistantId);

    // 대여 사진 업로드 (명세 55번)
    RentalImageUploadResponseDTO uploadRentalImage(
            Long rentalId, Long uploaderId, RentalImageType imageType, MultipartFile image);

    // 대여 반려 (명세 53번)
    RentalRejectResponseDTO rejectRental(Long rentalId, Long assistantId, RentalRejectRequestDTO dto);
}
