package com.linker.relia.insurance.service;

import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.insurance.dto.request.InsuranceManagementCompanyListRequest;
import com.linker.relia.insurance.dto.response.InsuranceManagementCompanyListItemResponse;

public interface InsuranceManagementService {
    PageResponse<InsuranceManagementCompanyListItemResponse> getInsuranceCompanies(
            InsuranceManagementCompanyListRequest request
    );
}
