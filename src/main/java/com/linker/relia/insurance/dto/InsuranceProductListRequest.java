package com.linker.relia.insurance.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InsuranceProductListRequest {
    private String insuranceCompanyCode;
    private String insuranceCategoryCode;
}
