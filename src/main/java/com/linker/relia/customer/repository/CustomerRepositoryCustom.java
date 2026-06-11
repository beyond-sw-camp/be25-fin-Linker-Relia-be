package com.linker.relia.customer.repository;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.customer.domain.CustomerStatus;
import com.linker.relia.customer.dto.CustomerDetailQueryResult;
import com.linker.relia.customer.dto.CustomerInterestItemResponse;
import com.linker.relia.customer.dto.CustomerInterestSummaryResponse;
import com.linker.relia.customer.dto.CustomerListItemResponse;
import com.linker.relia.customer.dto.CustomerListSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepositoryCustom {
    CustomerListSummaryResponse summarizeCustomers(AccessScope accessScope,
                                                   String organizationCode);

    CustomerInterestSummaryResponse summarizeInterestCustomers(AccessScope accessScope,
                                                               String organizationCode);

    Page<CustomerListItemResponse> searchCustomers(AccessScope accessScope,
                                                   String customerName,
                                                   String organizationCode,
                                                   CustomerStatus customerStatus,
                                                   Pageable pageable);

    Page<CustomerInterestItemResponse> searchInterestCustomers(AccessScope accessScope,
                                                               String customerName,
                                                               String organizationCode,
                                                               String interestReason,
                                                               Pageable pageable);

    boolean existsAccessibleCustomer(AccessScope accessScope, UUID customerId);

    Optional<CustomerDetailQueryResult> findCustomerDetail(AccessScope accessScope,
                                                           UUID customerId);
}
