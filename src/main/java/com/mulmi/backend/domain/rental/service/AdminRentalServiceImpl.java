package com.mulmi.backend.domain.rental.service;

import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;
import com.mulmi.backend.domain.rental.dto.request.RentalRejectRequestDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalApproveResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalRejectResponseDTO;
import com.mulmi.backend.domain.rental.entity.Rental;
import com.mulmi.backend.domain.rental.enums.RejectReasonType;
import com.mulmi.backend.domain.rental.enums.RentalStatus;
import com.mulmi.backend.domain.rental.exception.RentalException;
import com.mulmi.backend.domain.rental.exception.code.RentalErrorCode;
import com.mulmi.backend.domain.rental.repository.RentalRepository;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminRentalServiceImpl implements AdminRentalService {

    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    // 대여 승인
    // 격리 수준은 기본값 : 첫 쿼리가 잠금 조회라서, 잠금이 풀린 뒤의 최신 상태를 읽는다 (46번처럼 잠금 전에 일반 조회가 없음)
    @Override
    @Transactional
    public RentalApproveResponseDTO approveRental(Long rentalId, Long assistantId) {
        // 1. 대여 행 잠금 : 승인 두 번 클릭, 승인과 학생 취소가 동시에 와도 한 줄로 처리된다
        Rental rental = rentalRepository.findWithLockById(rentalId)
                .orElseThrow(() -> new RentalException(RentalErrorCode.RENTAL_NOT_FOUND));

        // 2. 승인 대기일 때만 승인할 수 있다 (먼저 처리된 요청이 상태를 바꿨으면 여기서 409)
        if (rental.getStatus() != RentalStatus.PENDING_APPROVAL) {
            throw new RentalException(RentalErrorCode.INVALID_RENTAL_STATUS);
        }

        // 3. 신청자 계정 상태 : 신청 뒤에 정지·탈퇴됐을 수 있다 (명세 52 처리 규칙 2)
        RentalServiceImpl.validateAccountStatus(rental.getUser());

        // 4. 블랙리스트 : 신청(46)과 같이 블랙리스트 담당 작업 후 추가

        // 5. 승인 : 조교는 FK 값만 필요하므로 조회 없이 참조만 만든다
        User assistant = userRepository.getReferenceById(assistantId);
        LocalDateTime now = LocalDateTime.now(KOREA);
        rental.approve(assistant, now);

        return new RentalApproveResponseDTO(rental.getId(), rental.getStatus(), rental.getReviewedAt());
    }

    // 대여 반려 (격리 수준 기본값인 이유는 승인과 같음)
    @Override
    @Transactional
    public RentalRejectResponseDTO rejectRental(Long rentalId, Long assistantId, RentalRejectRequestDTO dto) {
        // 1. 사유 검사 : DB를 보지 않고 끝나는 검사라 잠금보다 먼저 한다
        //    기타면 상세 사유 필수, 그 외 사유면 상세 사유는 무시하고 null (명세 53, 11-07 결정 A)
        RejectReasonType reasonType = dto.rejectReasonType();
        String reasonDetail = null;
        if (reasonType == RejectReasonType.OTHER) {
            if (dto.rejectReasonDetail() == null || dto.rejectReasonDetail().isBlank()) {
                throw new RentalException(RentalErrorCode.INVALID_REJECT_REASON);
            }
            reasonDetail = dto.rejectReasonDetail().trim();
        }

        // 2. 대여 행 잠금 + 승인 대기일 때만 반려 (승인과 같은 이유)
        Rental rental = rentalRepository.findWithLockById(rentalId)
                .orElseThrow(() -> new RentalException(RentalErrorCode.RENTAL_NOT_FOUND));
        if (rental.getStatus() != RentalStatus.PENDING_APPROVAL) {
            throw new RentalException(RentalErrorCode.INVALID_RENTAL_STATUS);
        }

        // 3. 신청 때 예약한 기기를 재고로 되돌린다 (명세 53, 10/15 결정)
        //    대여 흐름 중인 기기는 기기 수정(41)에서 상태를 바꿀 수 없으므로 여기서는 항상 RESERVED다 (11-07 결정)
        rental.getEquipmentItem().changeStatus(EquipmentItemStatus.AVAILABLE);

        // 4. 반려 기록
        User assistant = userRepository.getReferenceById(assistantId);
        rental.reject(assistant, LocalDateTime.now(KOREA), reasonType, reasonDetail);

        return new RentalRejectResponseDTO(
                rental.getId(), rental.getStatus(), rental.getRejectReasonType(), rental.getRejectReasonDetail());
    }
}
