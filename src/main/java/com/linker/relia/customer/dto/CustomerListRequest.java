package com.linker.relia.customer.dto;

import com.linker.relia.common.dto.request.PageQueryRequest;
import com.linker.relia.customer.domain.CustomerStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerListRequest extends PageQueryRequest {
    private String customerName;
    private String organizationCode;
    private CustomerStatus customerStatus;
    private Boolean interestYn;
}
