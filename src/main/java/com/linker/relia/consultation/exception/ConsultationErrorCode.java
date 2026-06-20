package com.linker.relia.consultation.exception;

import com.linker.relia.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ConsultationErrorCode implements ErrorCode {

    CONTRACT_REQUIRED(
            "CONSULTATION_001",
            "계약 ID는 필수입니다.",
            HttpStatus.BAD_REQUEST
    ),

    CONTRACT_NOT_ALLOWED(
            "CONSULTATION_002",
            "신규 계약 상담에는 계약 정보를 전달할 수 없습니다.",
            HttpStatus.BAD_REQUEST
    ),

    CONTRACT_NOT_FOUND(
            "CONSULTATION_003",
            "존재하지 않는 계약입니다.",
            HttpStatus.NOT_FOUND
    ),

    CONSULTATION_NOT_FOUND(
            "CONSULTATION_004",
            "존재하지 않는 상담일지입니다.",
            HttpStatus.NOT_FOUND
    ),

    CONSULTATION_ACCESS_DENIED(
            "CONSULTATION_005",
            "상담일지 조회 권한이 없습니다.",
            HttpStatus.FORBIDDEN
    ),

    CUSTOMER_TARGET_CONFLICT(
            "CONSULTATION_006",
            "customerId와 customerInfo를 동시에 전달할 수 없습니다.",
            HttpStatus.BAD_REQUEST
    ),

    CUSTOMER_INFO_NOT_ALLOWED(
            "CONSULTATION_007",
            "customerInfo는 NEW_CONTRACT 상담에서만 사용할 수 있습니다.",
            HttpStatus.BAD_REQUEST
    ),

    DUPLICATE_CUSTOMER_PHONE(
            "CONSULTATION_008",
            "동일한 휴대폰 번호의 고객이 이미 존재합니다.",
            HttpStatus.CONFLICT
    ),

    INVALID_DISEASE_CODE(
            "CONSULTATION_009",
            "유효하지 않은 기저질환 코드가 포함되어 있습니다.",
            HttpStatus.BAD_REQUEST
    ),

    CUSTOMER_TARGET_REQUIRED(
            "CONSULTATION_010",
            "NEW_CONTRACT 상담은 customerId 또는 customerInfo가 필요합니다.",
            HttpStatus.BAD_REQUEST
    ),

    CUSTOMER_ID_REQUIRED(
            "CONSULTATION_011",
            "CLAIM, RENEWAL, TERMINATION 상담은 customerId가 필요합니다.",
            HttpStatus.BAD_REQUEST
    ),

    CONSULTATION_STT_SESSION_NOT_FOUND(
            "CONSULTATION_012",
            "존재하지 않는 STT 세션입니다.",
            HttpStatus.NOT_FOUND
    ),

    CONSULTATION_STT_SESSION_ACCESS_DENIED(
            "CONSULTATION_013",
            "STT 세션 조회 권한이 없습니다.",
            HttpStatus.FORBIDDEN
    ),

    CONSULTATION_AI_NOTE_NOT_FOUND(
            "CONSULTATION_014",
            "AI 상담 초안을 찾을 수 없습니다.",
            HttpStatus.NOT_FOUND
    ),

    CONSULTATION_AI_NOTE_INVALID_DATA(
            "CONSULTATION_015",
            "AI 상담 초안 데이터가 올바르지 않습니다.",
            HttpStatus.INTERNAL_SERVER_ERROR
    );

    private final String code;
    private final String message;
    private final HttpStatus status;

    ConsultationErrorCode(String code, String message, HttpStatus status) {
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
