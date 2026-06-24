package com.linker.relia.contract.dto;

import com.linker.relia.common.dto.request.PageQueryRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ContractListRequest extends PageQueryRequest {
    private String organizationCode;
    private UUID insuranceCompanyId;
    private ContractListStatus contractStatus;
    private ContractListSort sort = ContractListSort.LATEST_CONTRACT;

}
