package com.linker.relia.customer.service;

import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.consultation.dto.request.ConsultationHistoryRequest;
import com.linker.relia.consultation.dto.response.ConsultationHistoryItemResponse;
import com.linker.relia.customer.dto.CustomerAiBriefingResponse;
import com.linker.relia.customer.dto.CustomerDetailResponse;
import com.linker.relia.customer.dto.CustomerFpHistoryItemResponse;
import com.linker.relia.customer.dto.CustomerFpHistoryRequest;
import com.linker.relia.customer.dto.CustomerInterestListRequest;
import com.linker.relia.customer.dto.CustomerInterestListResponse;
import com.linker.relia.customer.dto.CustomerListRequest;
import com.linker.relia.customer.dto.CustomerListResponse;
import com.linker.relia.customer.dto.CustomerOwnedContractResponse;
import com.linker.relia.security.principal.PrincipalDetails;

import java.util.List;
import java.util.UUID;

public interface CustomerService {
    CustomerListResponse getCustomers(PrincipalDetails principalDetails, CustomerListRequest request);

    CustomerInterestListResponse getInterestCustomers(PrincipalDetails principalDetails, CustomerInterestListRequest request);

    CustomerDetailResponse getCustomerDetail(PrincipalDetails principalDetails, UUID customerId);

    List<CustomerOwnedContractResponse> getOwnCustomerContracts(PrincipalDetails principalDetails, UUID customerId);

    PageResponse<ConsultationHistoryItemResponse> getOwnCustomerConsultations(PrincipalDetails principalDetails,
                                                                              UUID customerId,
                                                                              ConsultationHistoryRequest request);

    PageResponse<CustomerFpHistoryItemResponse> getCustomerFpHistories(PrincipalDetails principalDetails,
                                                                       UUID customerId,
                                                                       CustomerFpHistoryRequest request);

    CustomerAiBriefingResponse getCustomerAiBriefing(PrincipalDetails principalDetails, UUID customerId);
}
