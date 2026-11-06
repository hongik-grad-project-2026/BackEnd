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
import com.mulmi.backend.domain.equipment.entity.Category;
import com.mulmi.backend.domain.equipment.enums.LoanPeriodType;
import com.mulmi.backend.domain.rental.dto.response.RentalCheckoutResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalImageUploadResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalReturnResponseDTO;
import com.mulmi.backend.domain.rental.enums.ApplicationType;
import com.mulmi.backend.domain.semester.entity.Semester;
import com.mulmi.backend.domain.semester.repository.SemesterRepository;
import com.mulmi.backend.domain.rental.entity.RentalImage;
import com.mulmi.backend.domain.rental.enums.RentalImageSide;
import com.mulmi.backend.domain.rental.enums.RentalImageType;
import com.mulmi.backend.domain.rental.repository.RentalImageRepository;
import com.mulmi.backend.global.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminRentalServiceImpl implements AdminRentalService {

    private final RentalRepository rentalRepository;
    private final RentalImageRepository rentalImageRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final long MAX_RENTAL_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> RENTAL_IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

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
                rental.getActualPickupAt(),
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

    // 수령 처리 (명세 54번) : 격리 수준 기본값인 이유는 승인과 같음 (첫 쿼리가 잠금 조회)
    @Override
    @Transactional
    public RentalCheckoutResponseDTO checkoutRental(Long rentalId, Long staffId) {
        LocalDateTime now = LocalDateTime.now(KOREA);

        // 1. 대여 행 잠금 + 수령 대기일 때만
        Rental rental = rentalRepository.findWithLockById(rentalId)
                .orElseThrow(() -> new RentalException(RentalErrorCode.RENTAL_NOT_FOUND));
        if (rental.getStatus() != RentalStatus.READY_FOR_PICKUP) {
            throw new RentalException(RentalErrorCode.INVALID_RENTAL_STATUS);
        }

        // 2. 수령 기한 (11-03 규칙, 11-07 결정) : 지나면 노쇼라 수령할 수 없다. 일찍 오는 것은 허용
        //    사전신청 : 수령 주간 마지막 날 20:00 / 상시 : 수령 희망일 20:00
        Semester semester = rental.getSemester();
        LocalDateTime pickupDeadline = rental.getApplicationType() == ApplicationType.REGULAR
                ? semester.pickupDeadline()
                : Semester.closingAt(rental.getExpectedPickupDate());
        if (now.isAfter(pickupDeadline)) {
            throw new RentalException(RentalErrorCode.PICKUP_DEADLINE_PASSED);
        }

        // 3. 수령 사진 : 사진 필수 카테고리(노트북·태블릿)는 55번으로 올린 사진이 있어야 한다
        Category category = rental.getEquipmentModel().getCategory();
        if (category.isPickupPhotoRequired()
                && !rentalImageRepository.existsByRentalIdAndImageTypeAndImageSide(
                        rentalId, RentalImageType.PICKUP, RentalImageSide.FRONT)) {
            throw new RentalException(RentalErrorCode.PICKUP_IMAGE_REQUIRED);
        }

        // 4. 반납 예정 : 장기는 신청 때 정한 값 그대로, 단기는 (수령일 + 대여 일수) 20:00
        //    단, 이 학기의 최종 반납 마감(다음 학기 개강 전날 20:00)을 넘지 않게 자른다 (11-05 결정)
        LocalDateTime expectedReturnAt = rental.getExpectedReturnAt();
        if (category.getLoanPeriodType() == LoanPeriodType.FIXED_DAYS) {
            LocalDate nextSemesterStart = semesterRepository
                    .findFirstByStartDateGreaterThanOrderByStartDateAsc(semester.getStartDate())
                    .map(Semester::getStartDate)
                    .orElseGet(semester::defaultNextStartDate);
            LocalDateTime dueByDays = Semester.closingAt(now.toLocalDate().plusDays(category.getLoanDurationDays()));
            LocalDateTime semesterDue = Semester.returnDueAt(nextSemesterStart);
            expectedReturnAt = dueByDays.isAfter(semesterDue) ? semesterDue : dueByDays;
        }

        // 5. 기기 대여 중 + 수령 기록 (둘 다 변경 감지로 UPDATE)
        rental.getEquipmentItem().changeStatus(EquipmentItemStatus.RENTED);
        rental.pickUp(userRepository.getReferenceById(staffId), now, expectedReturnAt);

        return new RentalCheckoutResponseDTO(
                rental.getId(),
                rental.getEquipmentItem().getId(),
                rental.getEquipmentItem().getLabelCode(),
                rental.getStatus(),
                rental.getActualPickupAt(),
                rental.getExpectedReturnAt());
    }

    // 반납 처리 (명세 57번) : 격리 수준 기본값인 이유는 승인과 같음
    @Override
    @Transactional
    public RentalReturnResponseDTO returnRental(Long rentalId, Long staffId) {
        LocalDateTime now = LocalDateTime.now(KOREA);

        // 1. 대여 행 잠금 + 대여 중일 때만 (연체도 DB 상태는 RENTING). 조기 반납 허용
        Rental rental = rentalRepository.findWithLockById(rentalId)
                .orElseThrow(() -> new RentalException(RentalErrorCode.RENTAL_NOT_FOUND));
        if (rental.getStatus() != RentalStatus.RENTING) {
            throw new RentalException(RentalErrorCode.INVALID_RENTAL_STATUS);
        }

        // 2. 반납 사진 : 수령 사진 필수 카테고리(노트북·태블릿)만 반납 때도 필수 (11-07 결정)
        Category category = rental.getEquipmentModel().getCategory();
        if (category.isPickupPhotoRequired()
                && !rentalImageRepository.existsByRentalIdAndImageTypeAndImageSide(
                        rentalId, RentalImageType.RETURN, RentalImageSide.FRONT)) {
            throw new RentalException(RentalErrorCode.RETURN_IMAGE_REQUIRED);
        }

        // 3. 연체였는지 : 저장하지 않고 반납 시점에 계산한다
        boolean wasOverdue = rental.getExpectedReturnAt() != null && now.isAfter(rental.getExpectedReturnAt());
        // 연체 반납 시 블랙리스트 등록은 미정(명세 57) — 블랙리스트 담당 작업 후 협의

        // 4. 기기는 항상 대여 가능으로 되돌린다
        //    반납 때 고장을 발견해도 반납을 먼저 끝낸 뒤 기기 수정(41)으로 고장 처리한다 (11-07 결정)
        rental.getEquipmentItem().changeStatus(EquipmentItemStatus.AVAILABLE);
        rental.returnItem(userRepository.getReferenceById(staffId), now);

        return new RentalReturnResponseDTO(
                rental.getId(),
                rental.getEquipmentItem().getId(),
                rental.getStatus(),
                rental.getEquipmentItem().getStatus(),
                rental.getActualReturnAt(),
                wasOverdue);
    }

    // 대여 사진 업로드 (명세 55번) : 1장(FRONT)만, 같은 종류는 교체
    @Override
    @Transactional
    public RentalImageUploadResponseDTO uploadRentalImage(
            Long rentalId, Long uploaderId, RentalImageType imageType, MultipartFile image) {
        // 1. 파일 검사 : DB를 보지 않는 검사라 잠금보다 먼저
        validateRentalImage(image);

        // 2. 대여 행 잠금 + 상태 검사 : 수령 사진은 수령 대기, 반납 사진은 대여 중(연체 포함)일 때만
        //    수령·반납이 끝나면 상태가 바뀌어 사진을 더 이상 바꿀 수 없다 (ERD 사진 삭제 정책)
        Rental rental = rentalRepository.findWithLockById(rentalId)
                .orElseThrow(() -> new RentalException(RentalErrorCode.RENTAL_NOT_FOUND));
        RentalStatus required = imageType == RentalImageType.PICKUP
                ? RentalStatus.READY_FOR_PICKUP
                : RentalStatus.RENTING;
        if (rental.getStatus() != required) {
            throw new RentalException(RentalErrorCode.INVALID_RENTAL_STATUS);
        }

        // 3. S3 업로드
        String objectKey = s3StorageService.upload(image, "rental-images/" + rentalId);

        // 4. 같은 종류 사진이 있으면 교체, 없으면 새로 저장
        User uploader = userRepository.getReferenceById(uploaderId);
        RentalImage rentalImage = rentalImageRepository
                .findByRentalIdAndImageTypeAndImageSide(rentalId, imageType, RentalImageSide.FRONT)
                .orElse(null);
        String previousObjectKey = rentalImage == null ? null : rentalImage.getImageUrl();
        if (rentalImage == null) {
            rentalImage = RentalImage.builder()
                    .rental(rental)
                    .imageType(imageType)
                    .imageSide(RentalImageSide.FRONT)
                    .imageUrl(objectKey)
                    .uploadedBy(uploader)
                    .build();
        } else {
            rentalImage.replace(objectKey, uploader);
        }
        // flush로 지금 INSERT/UPDATE를 실행해 uploadedAt(updatedAt)을 채운다
        rentalImage = rentalImageRepository.saveAndFlush(rentalImage);

        // 5. S3는 롤백되지 않으므로 파일 정리는 트랜잭션 결과를 보고 한다 (모델 이미지 36번과 같은 방식)
        //    커밋 → 교체된 옛 파일 삭제 / 롤백 → 방금 올린 파일 삭제 / 결과 불명 → 둘 다 보존
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    deleteImageQuietly(previousObjectKey);
                } else if (status == STATUS_ROLLED_BACK) {
                    deleteImageQuietly(objectKey);
                }
            }
        });

        return new RentalImageUploadResponseDTO(
                rentalImage.getId(), rentalId, imageType,
                s3StorageService.createDownloadUrl(objectKey), rentalImage.getUpdatedAt());
    }

    // 대여 사진 검증 : JPG/PNG/WEBP, 10MB 이하 (11-07 결정 : 휴대폰 사진을 고려해 모델 이미지의 5MB보다 크게)
    private void validateRentalImage(MultipartFile image) {
        // Set.of는 contains(null)에서 NPE를 던지므로 null을 먼저 거른다 (36번에서 겪은 문제)
        if (image == null || image.isEmpty()
                || image.getContentType() == null
                || !RENTAL_IMAGE_CONTENT_TYPES.contains(image.getContentType())) {
            throw new RentalException(RentalErrorCode.INVALID_RENTAL_IMAGE);
        }
        if (image.getSize() > MAX_RENTAL_IMAGE_SIZE) {
            throw new RentalException(RentalErrorCode.RENTAL_IMAGE_TOO_LARGE);
        }
    }

    // S3 사진 삭제 (실패해도 요청 결과에는 영향 없음, 로그만 남김)
    private void deleteImageQuietly(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            s3StorageService.delete(objectKey);
        } catch (RuntimeException exception) {
            log.warn("대여 사진 삭제 실패: {}", objectKey, exception);
        }
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
