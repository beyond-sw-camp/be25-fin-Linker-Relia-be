package com.linker.relia.handover.dto.response;

import com.linker.relia.customer.domain.CustomerGrade;
import com.linker.relia.handover.domain.RequestStatus;
import com.linker.relia.handover.domain.RequestType;

import java.time.LocalDateTime;
import java.util.UUID;

public record HandoverListItemResponse (
    UUID handoverRequestId,
    String customerName,
    String recommendFpName,
    String organizationCode,
    CustomerGrade customerGrade,
    String currentFpName,
    RequestType requestType,
    RequestStatus requestStatus,
    LocalDateTime createdAt,
    LocalDateTime approvedAt
) {
    public static HandoverListItemResponse of(
            UUID handoverRequestId,
            String customerName,
            String recommendFpName,
            String organizationCode,
            CustomerGrade customerGrade,
            String currentFpName,
            RequestType requestType,
            RequestStatus requestStatus,
            LocalDateTime createdAt,
            LocalDateTime approvedAt) {
        return new HandoverListItemResponse(
                handoverRequestId, customerName, recommendFpName, organizationCode, customerGrade,
                currentFpName, requestType, requestStatus, createdAt, approvedAt
        );
    }
}
