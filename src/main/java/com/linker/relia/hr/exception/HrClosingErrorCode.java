package com.linker.relia.hr.exception;

import com.linker.relia.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum HrClosingErrorCode implements ErrorCode {
    HR_CLOSING_ALREADY_EXISTS("HR_001", "이미 마감된 기준 월입니다.", HttpStatus.CONFLICT),
    HR_CLOSING_MONTH_NOT_ALLOWED("HR_002", "현재 월 또는 미래 월은 인사 및 조직 마감할 수 없습니다.", HttpStatus.BAD_REQUEST),
    HR_CLOSING_COMMISSION_ALREADY_CLOSED("HR_003", "이미 수수료 마감된 기준 월은 인사 및 조직 마감할 수 없습니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;

    HrClosingErrorCode(String code, String message, HttpStatus status) {
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