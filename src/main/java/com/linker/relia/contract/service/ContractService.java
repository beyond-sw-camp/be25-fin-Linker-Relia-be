package com.linker.relia.contract.service;

import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.contract.dto.ContractListItemResponse;
import com.linker.relia.contract.dto.ContractListRequest;
import com.linker.relia.contract.dto.ContractSummaryRequest;
import com.linker.relia.contract.dto.ContractSummaryResponse;
import com.linker.relia.security.principal.PrincipalDetails;

public interface ContractService {
    ContractSummaryResponse getContractSummary(PrincipalDetails principalDetails,
                                               ContractSummaryRequest request);

    PageResponse<ContractListItemResponse> getContracts(PrincipalDetails principalDetails,
                                                        ContractListRequest request);
}
