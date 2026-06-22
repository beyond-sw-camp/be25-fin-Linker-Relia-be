package com.linker.relia.organization.exception;

import com.linker.relia.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum OrganizationErrorCode implements ErrorCode {
    ORGANIZATION_NOT_FOUND("ORG_001", "존재하지 않는 조직 코드입니다.", HttpStatus.NOT_FOUND),
    INVALID_BRANCH_ORGANIZATION("ORG_002", "설계사는 활성 지점에만 가입할 수 있습니다.", HttpStatus.BAD_REQUEST),
    FP_NOT_FOUND("ORG_003", "존재하지 않는 설계사입니다.", HttpStatus.NOT_FOUND),
    FP_ALREADY_RESIGNED("ORG_004", "이미 해촉 처리된 설계사입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;

    OrganizationErrorCode(String code, String message, HttpStatus status) {
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
