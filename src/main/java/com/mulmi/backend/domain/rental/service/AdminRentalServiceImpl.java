package com.mulmi.backend.domain.rental.service;

import com.mulmi.backend.domain.equipment.entity.EquipmentModel;
import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;
import com.mulmi.backend.domain.rental.dto.response.AdminRentalPageResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.AdminRentalSummaryResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    // 관리자 대여 목록 (조회라 클래스의 readOnly 트랜잭션을 그대로 쓴다)
    @Override
    public AdminRentalPageResponseDTO getAdminRentals(List<RentalStatus> statuses, String keyword, int page, int size) {
        LocalDateTime now = LocalDateTime.now(KOREA);

        // 1. 상태 필터 나누기 : RENTING·OVERDUE는 "RENTING 행을 연체 여부로 나눈 것"이라 따로 처리한다
        boolean allStatuses = statuses == null || statuses.isEmpty();
        boolean wantRenting = !allStatuses && statuses.contains(RentalStatus.RENTING);
        boolean wantOverdue = !allStatuses && statuses.contains(RentalStatus.OVERDUE);
        Set<RentalStatus> plainStatuses = allStatuses ? EnumSet.noneOf(RentalStatus.class) : EnumSet.copyOf(statuses);
        plainStatuses.removeAll(EnumSet.of(RentalStatus.RENTING, RentalStatus.OVERDUE));
        if (plainStatuses.isEmpty()) {
            // 빈 IN ()은 SQL 오류가 날 수 있어, DB에 저장되지 않는 OVERDUE를 넣어 "아무것도 안 걸리는" 조건으로 만든다
            plainStatuses.add(RentalStatus.OVERDUE);
        }

        // 2. 한 페이지 조회 (정렬은 쿼리의 ORDER BY r.id DESC = 신청 최신순)
        Page<AdminRentalSummaryResponseDTO> responsePage = rentalRepository.searchAdminRentals(
                        allStatuses, plainStatuses, wantRenting, wantOverdue, now,
                        normalizeKeyword(keyword), PageRequest.of(page, size))
                .map(rental -> toAdminSummary(rental, now));

        // 3. 탭 옆 숫자
        return AdminRentalPageResponseDTO.of(responsePage, countByStatus(now));
    }

    // 상태별 대여 수 + "all" 합계 (대여가 없는 상태도 0, RENTING 중 연체는 OVERDUE로 옮긴다)
    private Map<String, Long> countByStatus(LocalDateTime now) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("all", 0L);
        for (RentalStatus status : RentalStatus.values()) {
            counts.put(status.name(), 0L);
        }
        long total = 0;
        for (RentalRepository.StatusCount count : rentalRepository.countByStatus()) {
            counts.put(count.getStatus().name(), count.getCount());
            total += count.getCount();
        }
        long overdue = rentalRepository.countOverdue(now);
        counts.put(RentalStatus.RENTING.name(), counts.get(RentalStatus.RENTING.name()) - overdue);
        counts.put(RentalStatus.OVERDUE.name(), overdue);
        counts.put("all", total);
        return counts;
    }

    // 엔티티 → 목록 한 줄. 연관 엔티티는 fetch join으로 이미 들어와 있어 추가 쿼리가 없다
    private AdminRentalSummaryResponseDTO toAdminSummary(Rental rental, LocalDateTime now) {
        User user = rental.getUser();
        EquipmentModel model = rental.getEquipmentModel();
        return new AdminRentalSummaryResponseDTO(
                rental.getId(),
                user.getId(),
                user.getName(),
                user.getStudentId(),
                user.getDepartment(),
                model.getCategory().getName(),
                model.getId(),
                model.getName(),
                rental.getEquipmentItem().getLabelCode(),
                rental.getApplicationType(),
                displayStatus(rental, now),
                rental.getPurpose(),
                rental.getExpectedPickupDate(),
                rental.getExpectedReturnAt(),
                rental.getCreatedAt()
        );
    }

    // 화면에 보여줄 상태 : 연체는 저장하지 않고 계산한다 (RENTING + 반납 예정 시각 지남)
    private RentalStatus displayStatus(Rental rental, LocalDateTime now) {
        if (rental.getStatus() == RentalStatus.RENTING
                && rental.getExpectedReturnAt() != null
                && rental.getExpectedReturnAt().isBefore(now)) {
            return RentalStatus.OVERDUE;
        }
        return rental.getStatus();
    }

    // 빈 검색어는 조건 없음(null)으로
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
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
