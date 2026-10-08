package com.mulmi.backend.domain.equipment.dto.request;

import com.mulmi.backend.domain.equipment.enums.ApplicationPolicy;
import com.mulmi.backend.domain.equipment.enums.EquipmentModelStatus;
import com.mulmi.backend.domain.equipment.enums.LoanPeriodType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EquipmentModelCreateRequestDTO(
        @NotNull(message = "카테고리는 필수입니다.")
        Long categoryId,

        @NotNull(message = "라벨번호 접두사는 필수입니다.")
        Long prefixId,

        @Size(max = 100, message = "모델명은 100자를 넘을 수 없습니다.")
        @NotBlank(message = "모델명은 필수입니다.")
        String modelName,

        @NotBlank(message = "구성품은 필수입니다.")
        String components,

        String description,
        String specification,

        @NotNull(message = "상태는 필수입니다.")
        EquipmentModelStatus rentalStatus,

        @NotNull(message = "신청 방식은 필수입니다.")
        ApplicationPolicy applicationPolicy,

        @NotNull(message = "대여 기간 유형은 필수입니다.")
        LoanPeriodType loanPeriodType,

        Integer loanDurationDays,

        @NotNull(message = "서약서 필요 여부는 필수입니다.")
        Boolean pledgeRequired,

        @NotNull(message = "폐기일은 필수입니다.")
        LocalDate disposalDate
        ) {
}
