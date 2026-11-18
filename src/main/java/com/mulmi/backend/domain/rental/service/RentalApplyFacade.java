package com.mulmi.backend.domain.rental.service;

import com.mulmi.backend.domain.rental.dto.request.RentalCreateRequestDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalCreateResponseDTO;
import com.mulmi.backend.domain.rental.exception.RentalException;
import com.mulmi.backend.domain.rental.exception.code.RentalErrorCode;
import com.mulmi.backend.domain.rental.stock.RentalStockCounter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

// 대여 신청 문지기 : Redis 재고 숫자로 품절 요청을 DB(트랜잭션·커넥션)에 닿기 전에 돌려보낸다
// @Transactional을 붙이지 않는다
//  - 붙이면 여기서 DB 커넥션을 잡아 품절 요청도 커넥션을 쥐게 된다
//  - 안쪽 createRental이 이 트랜잭션에 합류해 커밋이 catch를 지난 뒤에 일어나므로, 커밋 실패를 되돌리지 못한다
@Slf4j
@Service
@RequiredArgsConstructor
public class RentalApplyFacade {

    private final RentalService rentalService;
    private final RentalStockCounter rentalStockCounter;

    public RentalCreateResponseDTO apply(Long userId, RentalCreateRequestDTO dto) {
        Long modelId = dto.modelId();
        Long remaining = tryDecrease(modelId);

        // Redis가 이 모델을 모름(키 없음·Redis 장애) → 문지기 없이 DB 잠금만으로 처리
        if (remaining == null) {
            return rentalService.createRental(userId, dto);
        }
        // 품절 : 숫자는 0에서 그대로 (스크립트가 빼지 않음)
        if (remaining < 0) {
            throw new RentalException(RentalErrorCode.OUT_OF_STOCK);
        }
        // 통과 : createRental은 커밋까지 끝내고 돌아오므로, 검증 실패·DB 오류·커밋 실패 모두 여기서 잡힌다
        try {
            return rentalService.createRental(userId, dto);
        } catch (RuntimeException exception) {
            restore(modelId);
            throw exception;
        }
    }

    private Long tryDecrease(Long modelId) {
        try {
            return rentalStockCounter.decrease(modelId);
        } catch (DataAccessException exception) {
            log.warn("대여 재고 차감 실패, DB로 처리: modelId={}", modelId, exception);
            return null;
        }
    }

    // 되돌리기가 실패해도 원래 예외(예: 409 중복 신청)가 그대로 응답되도록 여기서 삼킨다
    private void restore(Long modelId) {
        try {
            rentalStockCounter.increase(modelId);
        } catch (DataAccessException exception) {
            log.warn("대여 재고 되돌리기 실패, 숫자가 실제보다 1 적음: modelId={}", modelId, exception);
        }
    }
}
