package com.linker.relia.notification.exception;

import com.linker.relia.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum NotificationErrorCode implements ErrorCode {
    NOTIFICATION_NOT_FOUND("ALARM_001", "존재하지 않는 알림입니다.", HttpStatus.NOT_FOUND),
    NOTIFICATION_ACCESS_DENIED("ALARM_002", "해당 알림에 접근 권한이 없습니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;

    NotificationErrorCode(String code, String message, HttpStatus status) {
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
