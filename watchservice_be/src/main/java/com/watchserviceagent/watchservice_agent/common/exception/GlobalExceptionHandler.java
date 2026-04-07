package com.watchserviceagent.watchservice_agent.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 클래스 이름 : GlobalExceptionHandler
 * 기능 : 모든 컨트롤러에서 발생하는 예외를 한 곳에서 처리하여 일관된 에러 응답 형식을 반환한다.
 * 응답 형식 : { "code": "ERROR_CODE", "message": "설명" }
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        log.warn("[GlobalExceptionHandler] 리소스 없음: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "NOT_FOUND",
                             "message", e.getMessage() != null ? e.getMessage() : "리소스를 찾을 수 없습니다."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("입력값이 유효하지 않습니다.");
        log.warn("[GlobalExceptionHandler] 유효성 검사 실패: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", "VALIDATION_ERROR", "message", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[GlobalExceptionHandler] 잘못된 요청: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", "BAD_REQUEST",
                             "message", e.getMessage() != null ? e.getMessage() : "잘못된 요청입니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception e) {
        log.error("[GlobalExceptionHandler] 처리되지 않은 예외: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", "INTERNAL_ERROR", "message", "서버 내부 오류가 발생했습니다."));
    }
}
