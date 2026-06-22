package com.linker.relia.insurance.exception;

import com.linker.relia.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum InsuranceErrorCode implements ErrorCode {
    DUPLICATE_INSURANCE_COMPANY_CODE("INSURANCE_001", "이미 존재하는 보험사 코드입니다.", HttpStatus.CONFLICT),
    INSURANCE_COMPANY_NOT_FOUND("INSURANCE_002", "존재하지 않는 보험사입니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_INSURANCE_COMPANY_NAME("INSURANCE_003", "이미 존재하는 보험사명입니다.", HttpStatus.CONFLICT),
    INSURANCE_PRODUCT_NOT_FOUND("INSURANCE_004", "존재하지 않는 보험상품입니다.", HttpStatus.NOT_FOUND),
    INSURANCE_CATEGORY_NOT_FOUND("INSURANCE_005", "존재하지 않는 보종입니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_INSURANCE_PRODUCT_CODE("INSURANCE_006", "이미 존재하는 보험상품 식별 코드입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;

    InsuranceErrorCode(String code, String message, HttpStatus status) {
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
