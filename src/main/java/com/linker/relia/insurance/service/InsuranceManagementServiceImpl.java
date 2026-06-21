package com.linker.relia.insurance.service;

import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.insurance.dto.request.InsuranceManagementCompanyListRequest;
import com.linker.relia.insurance.dto.response.InsuranceManagementCompanyListItemResponse;
import com.linker.relia.insurance.repository.InsuranceCompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InsuranceManagementServiceImpl implements InsuranceManagementService {
    private final InsuranceCompanyRepository insuranceCompanyRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InsuranceManagementCompanyListItemResponse> getInsuranceCompanies(
            InsuranceManagementCompanyListRequest request
    ) {
        return PageResponse.from(
                insuranceCompanyRepository.searchManagementInsuranceCompanies(
                                request.normalizedInsuranceCompanyName(),
                                request.toPageable()
                        )
                        .map(InsuranceManagementCompanyListItemResponse::from)
        );
    }
}
