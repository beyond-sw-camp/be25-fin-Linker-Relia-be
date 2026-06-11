package com.linker.relia.consultation.exception;

import com.linker.relia.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ConsultationErrorCode implements ErrorCode {

    CONTRACT_REQUIRED(
            "CONSULTATION_001",
            "계약 ID는 필수입니다.",
            HttpStatus.BAD_REQUEST
    ),

    CONTRACT_NOT_ALLOWED(
            "CONSULTATION_002",
            "신규 상담은 계약 정보를 가질 수 없습니다.",
            HttpStatus.BAD_REQUEST
    ),

    CONTRACT_NOT_FOUND(
            "CONSULTATION_003",
            "존재하지 않는 계약입니다.",
            HttpStatus.NOT_FOUND
    ),

    CONSULTATION_NOT_FOUND(
        "CONSULTATION_004",
                "존재하지 않는 상담일지입니다.",
        HttpStatus.NOT_FOUND
    );

    private final String code;
    private final String message;
    private final HttpStatus status;

    ConsultationErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }
}