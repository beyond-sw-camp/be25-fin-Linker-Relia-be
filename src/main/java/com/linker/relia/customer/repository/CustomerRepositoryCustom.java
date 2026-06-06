package com.linker.relia.customer.repository;

import com.linker.relia.customer.domain.CustomerStatus;
import com.linker.relia.customer.dto.CustomerListItemResponse;
import com.linker.relia.customer.dto.CustomerListSummaryResponse;
import com.linker.relia.customer.policy.CustomerAccessScopeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerRepositoryCustom {
    CustomerListSummaryResponse summarizeCustomers(CustomerAccessScopeType scopeType,
                                                   UUID userId,
                                                   UUID organizationId,
                                                   String customerName,
                                                   String organizationCode,
                                                   CustomerStatus customerStatus);

    Page<CustomerListItemResponse> searchCustomers(CustomerAccessScopeType scopeType,
                                                   UUID userId,
                                                   UUID organizationId,
                                                   String customerName,
                                                   String organizationCode,
                                                   CustomerStatus customerStatus,
                                                   Pageable pageable);
}
