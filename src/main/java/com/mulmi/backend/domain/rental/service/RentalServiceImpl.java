package com.mulmi.backend.domain.rental.service;

import com.mulmi.backend.domain.equipment.entity.Category;
import com.mulmi.backend.domain.equipment.entity.EquipmentItem;
import com.mulmi.backend.domain.equipment.entity.EquipmentModel;
import com.mulmi.backend.domain.equipment.enums.EquipmentItemStatus;
import com.mulmi.backend.domain.equipment.enums.EquipmentModelStatus;
import com.mulmi.backend.domain.equipment.enums.LoanPeriodType;
import com.mulmi.backend.domain.equipment.exception.EquipmentModelException;
import com.mulmi.backend.domain.equipment.exception.code.EquipmentModelErrorCode;
import com.mulmi.backend.domain.equipment.repository.EquipmentItemRepository;
import com.mulmi.backend.domain.equipment.repository.EquipmentModelRepository;
import com.mulmi.backend.domain.rental.dto.request.RentalCreateRequestDTO;
import com.mulmi.backend.domain.rental.dto.request.RentalCancelRequestDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalCancelResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalCreateResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalDetailResponseDTO;
import com.mulmi.backend.domain.rental.entity.RentalImage;
import com.mulmi.backend.domain.rental.enums.RentalImageType;
import com.mulmi.backend.domain.rental.repository.RentalImageRepository;
import com.mulmi.backend.domain.rental.dto.response.RentalPageResponseDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalSummaryResponseDTO;
import com.mulmi.backend.domain.rental.entity.Rental;
import com.mulmi.backend.domain.rental.enums.ApplicationType;
import com.mulmi.backend.domain.rental.enums.RentalStatus;
import com.mulmi.backend.domain.rental.exception.RentalException;
import com.mulmi.backend.domain.rental.exception.code.RentalErrorCode;
import com.mulmi.backend.domain.rental.repository.RentalRepository;
import com.mulmi.backend.domain.semester.entity.Semester;
import com.mulmi.backend.domain.semester.repository.SemesterRepository;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.domain.user.enums.UserStatus;
import com.mulmi.backend.domain.user.exception.UserException;
import com.mulmi.backend.domain.user.exception.code.UserErrorCode;
import com.mulmi.backend.domain.user.repository.UserRepository;
import com.mulmi.backend.global.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentalServiceImpl implements RentalService {

    private final RentalRepository rentalRepository;
    private final SemesterRepository semesterRepository;
    private final EquipmentModelRepository equipmentModelRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;
    private final RentalImageRepository rentalImageRepository;

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    // READ_COMMITTED : MySQL 기본(REPEATABLE READ)이면 잠금을 기다린 뒤의 검사가
    // 트랜잭션 첫 조회 시점 스냅샷을 읽어, 앞 요청이 커밋한 대여·기기 상태를 보지 못한다
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RentalCreateResponseDTO createRental(Long userId, RentalCreateRequestDTO dto) {
        // 서버 컨테이너는 UTC라서 한국 시간을 직접 만든다 (학기 기간·20:00 판단 기준)
        LocalDateTime now = LocalDateTime.now(KOREA);

        // 1. 기간: 지금 학기 = 개강일이 오늘 이전인 학기 중 가장 최근
        //    사전신청(개강~7일) / 수령 기간(8~14일, 신청 불가) / 상시(15일~)
        Semester semester = semesterRepository
                .findFirstByStartDateLessThanEqualOrderByStartDateDesc(now.toLocalDate())
                .orElseThrow(() -> new RentalException(RentalErrorCode.RENTAL_NOT_OPEN));
        boolean preApply = semester.isPreApplyPeriod(now);
        if (!preApply && !semester.isOnDemandPeriod(now)) {
            throw new RentalException(RentalErrorCode.RENTAL_NOT_OPEN);
        }
        ApplicationType applicationType = preApply ? ApplicationType.REGULAR : ApplicationType.ON_DEMAND;

        // 다음 학기 개강일: 상시 수령희망일 상한(5단계)과 반납 마감 계산(9단계)에 쓴다
        LocalDate nextSemesterStart = semesterRepository
                .findFirstByStartDateGreaterThanOrderByStartDateAsc(semester.getStartDate())
                .map(Semester::getStartDate)
                .orElseGet(semester::defaultNextStartDate);

        // 2. 블랙리스트: 오늘이 제재 기간 안이면 403 (BlacklistRepository 생기면 추가 — 희빈이형 끝나면 시작)

        // 3. 모델: 존재하고 대여 가능(AVAILABLE)이어야 한다
        EquipmentModel model = equipmentModelRepository.findByIdAndDeletedAtIsNull(dto.modelId())
                .orElseThrow(() -> new EquipmentModelException(EquipmentModelErrorCode.MODEL_NOT_FOUND));
        if (model.getRentalStatus() != EquipmentModelStatus.AVAILABLE) {
            throw new RentalException(RentalErrorCode.MODEL_NOT_RENTABLE);
        }

        // 4. 카테고리 × 기간: 보조배터리·기타(정기대여 불가)는 상시 기간부터만
        Category category = model.getCategory();
        if (preApply && !category.isRegularRentalEnabled()) {
            throw new RentalException(RentalErrorCode.RENTAL_NOT_OPEN);
        }

        // 5. 수령희망일
        //    사전신청: 수령 기간(개강+7일 ~ +13일)의 날짜만
        //    상시: 오늘~모레(20:00 이후 신청이면 오늘 제외), 다음 학기 개강일 전까지
        LocalDate pickupDate = dto.expectedPickupDate();
        boolean pickupOk = preApply
                ? semester.isPickupDateSelectable(pickupDate)
                : semester.isOnDemandPickupDateSelectable(pickupDate, now)
                        && pickupDate.isBefore(nextSemesterStart);
        if (!pickupOk) {
            throw new RentalException(RentalErrorCode.INVALID_PICKUP_DATE);
        }

        // 6. 학생 행 잠금 (FOR UPDATE) — 같은 학생의 동시 신청을 한 줄로 세운다
        User user = userRepository.findWithLockById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        // 6-1. 계정 상태: 정지·탈퇴 계정은 토큰이 아직 유효해도 신청할 수 없다 (명세 46 처리 규칙 1)
        //      잠금으로 얻은 user를 그대로 쓰므로 조회가 더 나가지 않는다
        validateAccountStatus(user);

        // 7. 1인 1대: 같은 카테고리에 진행 중인 대여가 있으면 409
        if (rentalRepository.existsByUserIdAndEquipmentModelCategoryIdAndStatusIn(
                userId, category.getId(), RentalStatus.ACTIVE)) {
            throw new RentalException(RentalErrorCode.DUPLICATE_RENTAL);
        }

        // 8. 기기 잠금 (FOR UPDATE) — 대여 가능 기기 중 id가 가장 작은 1대, 없으면 409
        EquipmentItem item = equipmentItemRepository
                .findFirstByEquipmentModelIdAndStatusAndDeletedAtIsNullOrderByIdAsc(
                        model.getId(), EquipmentItemStatus.AVAILABLE)
                .orElseThrow(() -> new RentalException(RentalErrorCode.OUT_OF_STOCK));

        // 9. 기기 RESERVED + 대여 저장 → 응답
        // 기기 예약 : save() 없이 커밋 때 UPDATE (변경 감지)
        item.changeStatus(EquipmentItemStatus.RESERVED);

        // 장기(학기 단위)는 지금 반납 마감을 정하고, 단기는 수령할 때 정하므로 null
        LocalDateTime expectedReturnAt = category.getLoanPeriodType() == LoanPeriodType.SEMESTER_END
                ? Semester.returnDueAt(nextSemesterStart)
                : null;

        Rental rental = rentalRepository.save(Rental.builder()
                .user(user)
                .equipmentModel(model)
                .equipmentItem(item)
                .semester(semester)
                .applicationType(applicationType)
                .purpose(dto.purpose().trim())
                .expectedPickupDate(pickupDate)
                .expectedReturnAt(expectedReturnAt)
                .memo(dto.memo())
                .build());

        return new RentalCreateResponseDTO(
                rental.getId(), rental.getStatus(), rental.getApplicationType(), rental.getExpectedPickupDate());
    }

    // 내 대여 목록 (조회라 클래스의 readOnly 트랜잭션을 그대로 쓴다)
    // 흐름은 관리자 목록(50)과 같고, 쿼리가 로그인한 학생의 대여만 고른다
    @Override
    public RentalPageResponseDTO getMyRentals(Long userId, List<RentalStatus> statuses, String keyword, int page, int size) {
        LocalDateTime now = LocalDateTime.now(KOREA);

        // 1. 상태 필터 나누기 (50과 공유)
        RentalStatusFilter filter = RentalStatusFilter.of(statuses);

        // 2. 한 페이지 조회 (신청 최신순)
        Page<RentalSummaryResponseDTO> responsePage = rentalRepository.searchMyRentals(
                        userId, filter.allStatuses(), filter.plainStatuses(), filter.wantRenting(), filter.wantOverdue(), now,
                        AdminRentalServiceImpl.normalizeKeyword(keyword), PageRequest.of(page, size))
                .map(rental -> toSummary(rental, now));

        // 3. 탭 옆 숫자 (내 대여만)
        Map<String, Long> statusCounts = AdminRentalServiceImpl.toStatusCounts(
                rentalRepository.countByStatusAndUserId(userId),
                rentalRepository.countOverdueByUserId(userId, now));
        return RentalPageResponseDTO.of(responsePage, statusCounts);
    }

    // 내 대여 상세 (조회라 클래스의 readOnly 트랜잭션을 그대로 쓴다)
    @Override
    public RentalDetailResponseDTO getMyRental(Long userId, Long rentalId) {
        LocalDateTime now = LocalDateTime.now(KOREA);

        // 1. 내 대여 찾기 : 없거나 남의 대여면 똑같이 404 (명세 48 처리 규칙 2)
        Rental rental = rentalRepository.findMyRental(rentalId, userId)
                .orElseThrow(() -> new RentalException(RentalErrorCode.RENTAL_NOT_FOUND));

        // 2. 수령·반납 사진 : 종류별 S3 키 → 조회 URL (없으면 null)
        String pickupImageKey = null;
        String returnImageKey = null;
        for (RentalImage image : rentalImageRepository.findByRentalId(rental.getId())) {
            if (image.getImageType() == RentalImageType.PICKUP) {
                pickupImageKey = image.getImageUrl();
            } else if (image.getImageType() == RentalImageType.RETURN) {
                returnImageKey = image.getImageUrl();
            }
        }

        // 3. 응답 (모델·카테고리·기기는 fetch join으로 이미 들어와 있다)
        EquipmentModel model = rental.getEquipmentModel();
        return new RentalDetailResponseDTO(
                rental.getId(),
                model.getId(),
                model.getName(),
                s3StorageService.createDownloadUrl(model.getModelImageUrl()),
                model.getDescription(),
                model.getComponents(),
                model.getCategory().getName(),
                rental.getEquipmentItem().getLabelCode(),
                rental.getApplicationType(),
                rental.getDisplayStatus(now),
                rental.getPurpose(),
                rental.getMemo(),
                rental.getExpectedPickupDate(),
                rental.getReviewedAt(),
                rental.getActualPickupAt(),
                rental.getExpectedReturnAt(),
                rental.getActualReturnAt(),
                rental.getRejectReasonType(),
                rental.getRejectReasonDetail(),
                rental.getCancelReason(),
                rental.getCanceledAt(),
                s3StorageService.createDownloadUrl(pickupImageKey),
                s3StorageService.createDownloadUrl(returnImageKey),
                rental.getCreatedAt()
        );
    }

    // 신청 취소 (명세 49번) : 격리 수준 기본값인 이유는 승인(52)과 같다 — 첫 쿼리가 잠금 조회라 이전 스냅샷이 없다
    @Override
    @Transactional
    public RentalCancelResponseDTO cancelRental(Long userId, Long rentalId, RentalCancelRequestDTO dto) {
        // 1. 내 대여를 잠그며 찾기 : 조교 승인·반려, 근로생 수령 처리와 동시에 와도 한 줄로 선다
        Rental rental = rentalRepository.findWithLockByIdAndUserId(rentalId, userId)
                .orElseThrow(() -> new RentalException(RentalErrorCode.RENTAL_NOT_FOUND));

        // 2. 승인 대기·수령 대기만 취소 가능 (명세 49, 10/15 회의 확정)
        if (rental.getStatus() != RentalStatus.PENDING_APPROVAL
                && rental.getStatus() != RentalStatus.READY_FOR_PICKUP) {
            throw new RentalException(RentalErrorCode.INVALID_RENTAL_STATUS);
        }

        // 3. 신청 때 예약한 기기를 재고로 되돌린다 (반려 53과 같음 — 두 상태 모두 기기는 RESERVED)
        rental.getEquipmentItem().changeStatus(EquipmentItemStatus.AVAILABLE);

        // 4. 취소 기록
        rental.cancel(dto.cancelReason(), LocalDateTime.now(KOREA));

        return new RentalCancelResponseDTO(
                rental.getId(), rental.getStatus(), rental.getCancelReason(), rental.getCanceledAt());
    }

    // 엔티티 → 목록 한 줄. 모델·카테고리·기기는 fetch join으로 이미 들어와 있어 추가 쿼리가 없다
    private RentalSummaryResponseDTO toSummary(Rental rental, LocalDateTime now) {
        EquipmentModel model = rental.getEquipmentModel();
        return new RentalSummaryResponseDTO(
                rental.getId(),
                model.getId(),
                model.getName(),
                s3StorageService.createDownloadUrl(model.getModelImageUrl()),
                model.getCategory().getName(),
                rental.getEquipmentItem().getLabelCode(),
                rental.getApplicationType(),
                rental.getDisplayStatus(now),
                rental.getExpectedPickupDate(),
                rental.getActualPickupAt(),
                rental.getExpectedReturnAt(),
                rental.getActualReturnAt(),
                rental.getRejectReasonType(),
                rental.getRejectReasonDetail(),
                rental.getCancelReason(),
                rental.getCanceledAt(),
                rental.getCreatedAt()
        );
    }

    // 정지·탈퇴 계정 거부 (이유를 구분해서 알린다)
    // 승인(AdminRentalServiceImpl)에서도 같은 기준을 쓰므로 같은 패키지에서 부를 수 있게 static으로 둔다
    static void validateAccountStatus(User user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new RentalException(RentalErrorCode.ACCOUNT_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new RentalException(RentalErrorCode.ACCOUNT_WITHDRAWN);
        }
    }
}
