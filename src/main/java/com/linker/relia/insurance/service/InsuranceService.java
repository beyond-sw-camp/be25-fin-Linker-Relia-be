package com.linker.relia.insurance.service;

import com.linker.relia.insurance.dto.InsuranceCategoryResponse;
import com.linker.relia.insurance.dto.InsuranceCompanyResponse;
import com.linker.relia.insurance.dto.InsuranceProductListRequest;
import com.linker.relia.insurance.dto.InsuranceProductResponse;

import java.util.List;

public interface InsuranceService {
    List<InsuranceCompanyResponse> getInsuranceCompanies();

    List<InsuranceCategoryResponse> getInsuranceCategories();

    List<InsuranceProductResponse> getInsuranceProducts(InsuranceProductListRequest request);
}
