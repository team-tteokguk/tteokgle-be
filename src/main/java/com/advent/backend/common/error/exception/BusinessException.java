package com.advent.backend.common.error.exception;

/*
   최상위 커스텀 예외입니다.
*/

import com.advent.backend.common.error.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
