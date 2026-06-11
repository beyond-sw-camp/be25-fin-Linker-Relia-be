package com.linker.relia.customer.dto;

import com.linker.relia.common.dto.request.PageQueryRequest;
import com.linker.relia.customer.domain.InterestReason;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerInterestListRequest extends PageQueryRequest {
    private String customerName;
    private String organizationCode;
    private InterestReason interestReason;
}
