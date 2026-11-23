package com.mulmi.backend.domain.rental.service;

import com.mulmi.backend.domain.rental.dto.request.RentalCreateRequestDTO;
import com.mulmi.backend.domain.rental.dto.response.RentalCreateResponseDTO;
import com.mulmi.backend.domain.rental.exception.RentalException;
import com.mulmi.backend.domain.rental.exception.code.RentalErrorCode;
import com.mulmi.backend.domain.rental.stock.RentalStockCounter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 대여 신청 문지기 : Redis 재고 숫자로 품절 요청을 DB(트랜잭션·커넥션)에 닿기 전에 돌려보낸다
// @Transactional을 붙이지 않는다
//  - 붙이면 여기서 DB 커넥션을 잡아 품절 요청도 커넥션을 쥐게 된다
//  - 안쪽 createRental이 이 트랜잭션에 합류해 커밋이 catch를 지난 뒤에 일어나므로, 커밋 실패를 되돌리지 못한다
@Service
@RequiredArgsConstructor
public class RentalApplyFacade {

    private final RentalService rentalService;
    private final RentalStockCounter rentalStockCounter;

    public RentalCreateResponseDTO apply(Long userId, RentalCreateRequestDTO dto) {
        Long modelId = dto.modelId();
        // 차감 전에 받는다 : DB 품절 때 "숫자가 컸던 원인의 콜백이 아직 올 수 있는지" 판단에 쓴다
        long changeToken = rentalStockCounter.changeToken(modelId);
        Long remaining = rentalStockCounter.decrease(modelId);

        // Redis가 이 모델을 모름(키 없음·첫 재동기화 전·Redis 장애·차감 실패) → 문지기 없이 DB 잠금만으로 처리
        //  차감 실패는 decrease가 키를 지워 두므로, 실제로 빠졌더라도 되돌릴 필요가 없다
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
        } catch (RentalException exception) {
            // DB가 기기 잠금으로 "대여 가능 기기 없음"을 확인했다 = Redis 숫자가 실제보다 컸다
            //  (잠금은 건너뛰지 않고 기다리므로(SKIP LOCKED 아님) 커밋된 상태 기준의 진짜 품절)
            //  되돌리면 숫자가 그대로 남아 다음 신청도 Redis를 통과해 DB에서 거절되기를 반복한다
            //  → 되돌리지 않는다. 뺀 1을 둘지, 키를 지울지는 Counter가 늦은 콜백 가능성을 보고 정한다
            if (exception.getCode() == RentalErrorCode.OUT_OF_STOCK) {
                rentalStockCounter.onDatabaseOutOfStock(modelId, changeToken);
            } else {
                restore(modelId);
            }
            throw exception;
        } catch (RuntimeException exception) {
            restore(modelId);
            throw exception;
        }
    }

    // 되돌리기는 예외를 던지지 않는다 : 실패하면 increase 안에서 이 모델을 DB 판단으로 돌린다
    //  (원래 예외, 예: 409 중복 신청이 그대로 응답되어야 하므로)
    private void restore(Long modelId) {
        rentalStockCounter.increase(modelId);
    }
}
