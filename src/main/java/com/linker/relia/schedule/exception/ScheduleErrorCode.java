package com.linker.relia.schedule.exception;

import com.linker.relia.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ScheduleErrorCode implements ErrorCode {

    SCHEDULE_NOT_FOUND("SCHEDULE_001", "상담 일정을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SCHEDULE_ACCESS_DENIED("SCHEDULE_002", "해당 상담 일정에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ScheduleErrorCode(String code, String message, HttpStatus status) {
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