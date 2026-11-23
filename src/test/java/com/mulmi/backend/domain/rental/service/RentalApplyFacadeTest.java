package com.mulmi.backend.domain.rental.service;

import com.mulmi.backend.domain.rental.dto.request.RentalCreateRequestDTO;
import com.mulmi.backend.domain.rental.exception.RentalException;
import com.mulmi.backend.domain.rental.exception.code.RentalErrorCode;
import com.mulmi.backend.domain.rental.stock.RentalStockCounter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RentalApplyFacadeTest {

    private static final Long USER = 1L;
    private static final Long MODEL = 900L;

    @Mock
    private RentalService rentalService;

    @Mock
    private RentalStockCounter rentalStockCounter;

    @InjectMocks
    private RentalApplyFacade facade;

    // Redis는 통과했는데 DB가 품절을 확인 = Redis 숫자가 컸다 → 되돌리지 않고, 차감 전 표와 함께 Counter에 알린다
    @Test
    void databaseOutOfStockIsReportedWithTokenInsteadOfRestore() {
        RentalCreateRequestDTO dto = request();
        given(rentalStockCounter.changeToken(MODEL)).willReturn(7L);
        given(rentalStockCounter.decrease(MODEL)).willReturn(0L);
        given(rentalService.createRental(USER, dto)).willThrow(new RentalException(RentalErrorCode.OUT_OF_STOCK));

        assertThatThrownBy(() -> facade.apply(USER, dto)).isInstanceOf(RentalException.class);
        verify(rentalStockCounter).onDatabaseOutOfStock(MODEL, 7L);
        verify(rentalStockCounter, never()).increase(MODEL);
    }

    // 품절이 아닌 실패(중복 신청 등)는 기기를 쓰지 않았으므로 뺀 1을 되돌린다
    @Test
    void otherFailureRestoresRedisDecrement() {
        RentalCreateRequestDTO dto = request();
        given(rentalStockCounter.decrease(MODEL)).willReturn(0L);
        given(rentalService.createRental(USER, dto)).willThrow(new RentalException(RentalErrorCode.DUPLICATE_RENTAL));

        assertThatThrownBy(() -> facade.apply(USER, dto)).isInstanceOf(RentalException.class);
        verify(rentalStockCounter).increase(MODEL);
    }

    private RentalCreateRequestDTO request() {
        return new RentalCreateRequestDTO(MODEL, "수업", LocalDate.of(2026, 12, 1), null);
    }
}
