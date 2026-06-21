package com.linker.relia.insurance.service;

import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.insurance.dto.request.InsuranceCompanyCreateRequest;
import com.linker.relia.insurance.dto.request.InsuranceCompanyUpdateRequest;
import com.linker.relia.insurance.dto.request.InsuranceManagementCompanyListRequest;
import com.linker.relia.insurance.dto.request.InsuranceManagementProductListRequest;
import com.linker.relia.insurance.dto.response.InsuranceCompanyCreateResponse;
import com.linker.relia.insurance.dto.response.InsuranceCompanyDetailResponse;
import com.linker.relia.insurance.dto.response.InsuranceManagementCompanyListItemResponse;
import com.linker.relia.insurance.dto.response.InsuranceManagementProductListItemResponse;

import java.util.UUID;

public interface InsuranceManagementService {
    PageResponse<InsuranceManagementCompanyListItemResponse> getInsuranceCompanies(
            InsuranceManagementCompanyListRequest request
    );

    InsuranceCompanyCreateResponse createInsuranceCompany(InsuranceCompanyCreateRequest request);

    InsuranceCompanyDetailResponse getInsuranceCompanyDetail(UUID insuranceCompanyId);

    InsuranceCompanyDetailResponse updateInsuranceCompany(UUID insuranceCompanyId, InsuranceCompanyUpdateRequest request);

    PageResponse<InsuranceManagementProductListItemResponse> getInsuranceProducts(
            InsuranceManagementProductListRequest request
    );
}
