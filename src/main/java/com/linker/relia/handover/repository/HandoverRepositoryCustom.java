package com.linker.relia.handover.repository;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.handover.domain.RequestStatus;
import com.linker.relia.handover.domain.RequestType;
import com.linker.relia.handover.dto.response.HandoverListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HandoverRepositoryCustom {

    Page<HandoverListItemResponse> searchHandovers(
            AccessScope accessScope,
            RequestStatus status,
            RequestType requestType,
            String customerName,
            Pageable pageable
    );
}