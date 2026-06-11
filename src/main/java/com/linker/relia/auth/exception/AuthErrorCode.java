package com.linker.relia.auth.exception;

import com.linker.relia.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {
    USER_UNAUTHORIZED("USER_AUTH_001", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    USER_FORBIDDEN("USER_AUTH_002", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    INVALID_LOGIN_CREDENTIALS("USER_AUTH_003", "아이디 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_LOGIN_REQUEST("USER_AUTH_005", "로그인 요청 파싱 실패", HttpStatus.UNAUTHORIZED),
    INVALID_USER_STATE("USER_AUTH_006", "유효하지 않은 사용자 상태입니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    ACCESS_TOKEN_NOT_FOUND("AUTH_ACCESS_001", "Access Token을 요청받지 못했습니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_TOKEN_EXPIRED("AUTH_ACCESS_002", "만료된 Access Token입니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_TOKEN_INVALID("AUTH_ACCESS_003", "유효하지 않은 Access Token입니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_TOKEN_OF_BLACK_LIST("AUTH_ACCESS_004", "해당 Access Token은 블랙리스트 토큰입니다.", HttpStatus.CONFLICT),

    REFRESH_TOKEN_NOT_FOUND("AUTH_REFRESH_001", "Refresh Token을 요청받지 못했습니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED("AUTH_REFRESH_002", "만료된 Refresh Token입니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID("AUTH_REFRESH_003", "유효하지 않은 Refresh Token입니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_IN_REDIS("AUTH_REFRESH_004", "Refresh Token이 Redis에 존재하지 않습니다.", HttpStatus.UNAUTHORIZED);

    private final String code;
    private final String message;
    private final HttpStatus status;

    AuthErrorCode(String code, String message, HttpStatus status) {
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
