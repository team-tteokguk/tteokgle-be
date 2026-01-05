package com.advent.backend.common.error;

/*
   프로젝트 전역에서 발생하는 예외를 잡아 JSON으로 응답해주는 클래스입니다.
*/

import com.advent.backend.common.error.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleException(BusinessException e) {
        log.error("BusinessException {}", e.getErrorCode().getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("UnHandled Exception", e);
        return ResponseEntity.status(500).body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
