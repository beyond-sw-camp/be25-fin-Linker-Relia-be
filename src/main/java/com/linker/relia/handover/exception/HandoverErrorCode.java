package com.linker.relia.handover.exception;

import com.linker.relia.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum HandoverErrorCode implements ErrorCode {

    CUSTOMER_NOT_FOUND("HANDOVER_001", "존재하지 않는 고객입니다.", HttpStatus.NOT_FOUND),
    HANDOVER_ALREADY_IN_PROGRESS("HANDOVER_002", "이미 진행 중인 인수인계 요청이 존재합니다.", HttpStatus.CONFLICT),
    NO_AVAILABLE_FP("HANDOVER_003", "추천 가능한 설계사가 없습니다.", HttpStatus.UNPROCESSABLE_ENTITY),
    FP_NOT_FOUND("HANDOVER_004", "존재하지 않는 설계사입니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;

    HandoverErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
    @Override public HttpStatus getStatus() { return status; }
}