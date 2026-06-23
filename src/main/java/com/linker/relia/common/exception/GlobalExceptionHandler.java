package com.linker.relia.common.exception;

import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.common.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        log.warn("비즈니스 예외 발생. errorCode={}, message={}", errorCode.getCode(), exception.getMessage());
        return ApiResponse.failure(errorCode, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .filter(defaultMessage -> defaultMessage != null && !defaultMessage.isBlank())
                .orElse(CommonErrorCode.INVALID_REQUEST.getMessage());

        log.warn("요청 값 검증 예외 발생. message={}", message);
        return ApiResponse.failure(CommonErrorCode.INVALID_REQUEST, message);
    }

    /*
      - 잘못된 요청으로 간주하는 예외를 한 곳에서 공통 처리한다.
      - ConstraintViolationException
        - @RequestParam, @PathVariable 등에 선언한 검증 조건을 만족하지 못한 경우
      - HttpMessageNotReadableException
        - 요청 본문(JSON)을 읽을 수 없거나 형식이 올바르지 않은 경우
      - MethodArgumentTypeMismatchException
        - 요청 파라미터를 지정한 타입으로 변환하지 못한 경우
      - IllegalArgumentException
        - 잘못된 인자 값이 전달된 경우
      - 위 예외들은 모두 클라이언트 요청 형식 또는 값 오류로 보고 INVALID_REQUEST 응답으로 통일한다.
     */
    @ExceptionHandler({ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(Exception exception) {
        log.warn("잘못된 요청 예외 발생.", exception);
        return ApiResponse.failure(CommonErrorCode.INVALID_REQUEST, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception
    ) {
        log.warn("데이터 제약조건 위반이 발생했습니다.", exception);
        return ApiResponse.failure(CommonErrorCode.INVALID_REQUEST, "요청 값이 데이터 제약조건을 위반했습니다.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException exception) {
        log.warn("접근 권한이 없습니다.", exception);
        return ApiResponse.failure(AuthErrorCode.USER_FORBIDDEN, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        log.error("처리되지 않은 예외가 발생했습니다.", exception);
        return ApiResponse.failure(CommonErrorCode.INTERNAL_SERVER_ERROR, null);
    }
}
