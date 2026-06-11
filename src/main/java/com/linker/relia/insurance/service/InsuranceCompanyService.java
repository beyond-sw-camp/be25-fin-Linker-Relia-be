package com.linker.relia.insurance.service;

import com.linker.relia.insurance.dto.request.InsuranceCompanyListRequest;
import com.linker.relia.insurance.dto.response.InsuranceCompanyPageResponse;

public interface InsuranceCompanyService {
    InsuranceCompanyPageResponse getPartnerInsuranceCompanies(InsuranceCompanyListRequest request);
}
