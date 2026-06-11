package com.linker.relia.contract.service;

import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.contract.dto.ContractDetailResponse;
import com.linker.relia.contract.dto.ContractListItemResponse;
import com.linker.relia.contract.dto.ContractListRequest;
import com.linker.relia.contract.dto.ContractMonthlyTrendResponse;
import com.linker.relia.contract.dto.ContractSummaryRequest;
import com.linker.relia.contract.dto.ContractSummaryResponse;
import com.linker.relia.contract.dto.InsuranceCompanyContractStatusResponse;
import com.linker.relia.security.principal.PrincipalDetails;

import java.util.List;
import java.util.UUID;

public interface ContractService {
    ContractSummaryResponse getContractSummary(PrincipalDetails principalDetails,
                                               ContractSummaryRequest request);

    PageResponse<ContractListItemResponse> getContracts(PrincipalDetails principalDetails,
                                                        ContractListRequest request);

    List<InsuranceCompanyContractStatusResponse> getInsuranceCompanyContractStatuses(PrincipalDetails principalDetails,
                                                                                     ContractSummaryRequest request);

    List<ContractMonthlyTrendResponse> getMonthlyContractTrend(PrincipalDetails principalDetails,
                                                               ContractSummaryRequest request);

    ContractDetailResponse getContractDetail(PrincipalDetails principalDetails,
                                             UUID contractId);
}
