package com.mulmi.backend.domain.equipment.exception;

import com.mulmi.backend.global.apiPayload.code.BaseErrorCode;
import com.mulmi.backend.global.apiPayload.exception.GeneralException;

public class EquipmentModelException extends GeneralException {
  public EquipmentModelException(BaseErrorCode code) {
    super(code);
  }
}
