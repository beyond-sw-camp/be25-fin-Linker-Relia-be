package com.linker.relia.handover.repository;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.handover.domain.RequestStatus;
import com.linker.relia.handover.domain.RequestType;
import com.linker.relia.handover.dto.response.HandoverListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface HandoverRepositoryCustom {

    Page<HandoverListItemResponse> searchHandovers(
            AccessScope accessScope,
            RequestStatus status,
            RequestType requestType,
            String customerName,
            Pageable pageable
    );
    List<String> findCustomerCategories(UUID customerId);
    String findMainChannel(UUID customerId);
}