package com.linker.relia.handover.exception;

import com.linker.relia.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum HandoverErrorCode implements ErrorCode {

    CUSTOMER_NOT_FOUND("HANDOVER_001", "존재하지 않는 고객입니다.", HttpStatus.NOT_FOUND),
    HANDOVER_REQUEST_NOT_FOUND("HANDOVER_002", "인수인계 요청을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    HANDOVER_REQUEST_ALREADY_EXISTS("HANDOVER_003", "이미 진행 중인 인수인계 요청이 존재합니다.", HttpStatus.CONFLICT),
    NO_AVAILABLE_FP("HANDOVER_004", "추천 가능한 설계사가 없습니다.", HttpStatus.BAD_REQUEST),
    FP_NOT_FOUND("HANDOVER_005", "존재하지 않는 설계사입니다.", HttpStatus.NOT_FOUND),
    RECOMMENDATION_NOT_FOUND("HANDOVER_006", "추천 결과를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_APPROVAL_REQUEST("HANDOVER_007", "결재 요청 값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    HANDOVER_ALREADY_COMPLETED("HANDOVER_008", "이미 완료된 인수인계 요청입니다.", HttpStatus.CONFLICT),
    INVALID_HANDOVER_APPROVAL_TARGET("HANDOVER_009", "결재할 수 없는 인수인계 요청 상태입니다.", HttpStatus.CONFLICT),
    COMMENT_GENERATION_FAILED("HANDOVER_010", "추천 이유 멘트 생성에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;

    HandoverErrorCode(String code, String message, HttpStatus status) {
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
