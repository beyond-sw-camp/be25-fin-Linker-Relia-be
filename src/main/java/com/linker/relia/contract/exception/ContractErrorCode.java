package com.linker.relia.contract.exception;

import com.linker.relia.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ContractErrorCode implements ErrorCode {
    CONTRACT_NOT_FOUND("CONTRACT_001", "존재하지 않는 계약입니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_CONTRACT_CODE("CONTRACT_002", "이미 존재하는 계약번호입니다.", HttpStatus.CONFLICT),
    INVALID_CONTRACT_DATE("CONTRACT_003", "계약 날짜가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_PAYMENT_CYCLE("CONTRACT_004", "납입 주기는 MONTHLY만 지원합니다.", HttpStatus.BAD_REQUEST),
    INSURANCE_PRODUCT_NOT_FOUND("CONTRACT_005", "존재하지 않는 보험상품입니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_ACTIVE_OR_LAPSED_CONTRACT("CONTRACT_006", "이미 유지 또는 실효 중인 동일 보험상품 계약이 존재합니다.", HttpStatus.CONFLICT);

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
