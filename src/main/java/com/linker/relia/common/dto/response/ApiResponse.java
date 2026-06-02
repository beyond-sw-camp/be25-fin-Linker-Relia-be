package com.linker.relia.common.dto.response;

import com.linker.relia.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private final int status;
    private final String errorCode;
    private final String message;
    private final T result;

    public static <T> ResponseEntity<ApiResponse<T>> success(HttpStatus httpStatus, String message, T result) {
        return ResponseEntity.status(httpStatus).body(successBody(httpStatus, message, result));
    }

    public static <T> ApiResponse<T> successBody(HttpStatus httpStatus, String message, T result) {
        return ApiResponse.<T>builder()
                .status(httpStatus.value())
                .errorCode(null)
                .message(message)
                .result(result)
                .build();
    }

    public static ResponseEntity<ApiResponse<Void>> failure(ErrorCode errorCode, String message) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(failureBody(errorCode, message));
    }

    public static ApiResponse<Void> failureBody(ErrorCode errorCode, String message) {
        return ApiResponse.<Void>builder()
                .status(errorCode.getStatus().value())
                .errorCode(errorCode.getCode())
                .message((message == null || message.isBlank()) ? errorCode.getMessage() : message)
                .result(null)
                .build();
    }
}
