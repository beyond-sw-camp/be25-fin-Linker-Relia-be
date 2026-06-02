package com.linker.relia.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    USER_UNAUTHORIZED("USER_AUTH_001", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    USER_FORBIDDEN("USER_AUTH_002", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    INVALID_LOGIN_CREDENTIALS("USER_AUTH_003", "아이디 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    USER_RESIGNED("USER_AUTH_004", "퇴직 또는 해촉된 사용자입니다.", HttpStatus.FORBIDDEN),
    INVALID_LOGIN_REQUEST("USER_AUTH_005", "로그인 요청 파싱 실패", HttpStatus.UNAUTHORIZED),

    ACCESS_TOKEN_NOT_FOUND("auth_access_001", "Access Token을 요청받지 못했습니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_TOKEN_EXPIRED("auth_access_002", "만료된 Access Token입니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_TOKEN_INVALID("auth_access_003", "유효하지 않은 Access Token입니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_TOKEN_OF_BLACK_LIST("auth_access_004", "해당 Access Token은 BlackList 토큰입니다.", HttpStatus.CONFLICT),

    REFRESH_TOKEN_NOT_FOUND("auth_refresh_001", "Refresh Token을 요청받지 못했습니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED("auth_refresh_002", "만료된 Refresh Token입니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID("auth_refresh_003", "유효하지 않은 Refresh Token입니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_IN_REDIS("auth_refresh_004", "Refresh Token이 Redis에 존재하지 않습니다.", HttpStatus.UNAUTHORIZED),

    INVALID_REQUEST("COMMON_001", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("COMMON_002", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
