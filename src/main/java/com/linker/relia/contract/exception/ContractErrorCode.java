package com.linker.relia.contract.exception;

import com.linker.relia.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ContractErrorCode implements ErrorCode {
    CONTRACT_NOT_FOUND("CONTRACT_001", "존재하지 않는 계약입니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ContractErrorCode(String code, String message, HttpStatus status) {
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
