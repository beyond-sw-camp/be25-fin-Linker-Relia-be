package com.linker.relia.customer.service;

import com.linker.relia.customer.dto.CustomerDetailResponse;
import com.linker.relia.customer.dto.CustomerListRequest;
import com.linker.relia.customer.dto.CustomerListResponse;
import com.linker.relia.security.principal.PrincipalDetails;

import java.util.UUID;

public interface CustomerService {
    CustomerListResponse getCustomers(PrincipalDetails principalDetails, CustomerListRequest request);

    CustomerDetailResponse getCustomerDetail(PrincipalDetails principalDetails, UUID customerId);
}
