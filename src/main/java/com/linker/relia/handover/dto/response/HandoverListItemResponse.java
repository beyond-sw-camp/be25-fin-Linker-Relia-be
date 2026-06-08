package com.linker.relia.handover.dto.response;

import com.linker.relia.customer.domain.CustomerGrade;
import com.linker.relia.handover.domain.RequestStatus;
import com.linker.relia.handover.domain.RequestType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class HandoverListItemResponse {

    private final String handoverRequestId;
    private final String customerName;
    private final CustomerGrade customerGrade;
    private final String currentFpName;
    private final RequestType requestType;
    private final RequestStatus requestStatus;
    private final LocalDateTime createdAt;

    // JPQL new 생성자 - 필드 순서가 쿼리랑 정확히 일치해야 함
    public HandoverListItemResponse(String handoverRequestId,
                                    String customerName,
                                    CustomerGrade customerGrade,
                                    String currentFpName,
                                    RequestType requestType,
                                    RequestStatus requestStatus,
                                    LocalDateTime createdAt) {
        this.handoverRequestId = handoverRequestId;
        this.customerName = customerName;
        this.customerGrade = customerGrade;
        this.currentFpName = currentFpName;
        this.requestType = requestType;
        this.requestStatus = requestStatus;
        this.createdAt = createdAt;
    }
}
