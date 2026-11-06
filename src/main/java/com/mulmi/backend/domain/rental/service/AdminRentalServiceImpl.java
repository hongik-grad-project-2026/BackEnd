package com.mulmi.backend.domain.rental.service;

import com.mulmi.backend.domain.rental.dto.response.RentalApproveResponseDTO;
import com.mulmi.backend.domain.rental.entity.Rental;
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
}
