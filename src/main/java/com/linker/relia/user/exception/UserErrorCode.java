package com.linker.relia.user.exception;

import com.linker.relia.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND("USER_001", "존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),
    USER_RESIGNED("USER_002", "재직 중이 아니거나 탈퇴한 사용자입니다.", HttpStatus.FORBIDDEN),
    DUPLICATE_LOGIN_ID("USER_003", "이미 사용 중인 로그인 ID입니다.", HttpStatus.CONFLICT),
    DUPLICATE_EMAIL("USER_004", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    EMP_CODE_SEQUENCE_NOT_FOUND("USER_005", "사번 시퀀스 설정을 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;

    UserErrorCode(String code, String message, HttpStatus status) {
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
