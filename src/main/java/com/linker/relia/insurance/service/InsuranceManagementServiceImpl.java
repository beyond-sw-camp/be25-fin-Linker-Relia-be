package com.linker.relia.insurance.service;

import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.insurance.domain.InsuranceCompany;
import com.linker.relia.insurance.dto.request.InsuranceCompanyCreateRequest;
import com.linker.relia.insurance.dto.request.InsuranceCompanyUpdateRequest;
import com.linker.relia.insurance.dto.request.InsuranceManagementCompanyListRequest;
import com.linker.relia.insurance.dto.request.InsuranceManagementProductListRequest;
import com.linker.relia.insurance.dto.response.InsuranceCompanyCreateResponse;
import com.linker.relia.insurance.dto.response.InsuranceCompanyDetailResponse;
import com.linker.relia.insurance.dto.response.InsuranceManagementCompanyListItemResponse;
import com.linker.relia.insurance.dto.response.InsuranceManagementProductListItemResponse;
import com.linker.relia.insurance.dto.response.InsuranceProductDetailResponse;
import com.linker.relia.insurance.exception.InsuranceErrorCode;
import com.linker.relia.insurance.repository.InsuranceCompanyRepository;
import com.linker.relia.insurance.repository.InsuranceProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InsuranceManagementServiceImpl implements InsuranceManagementService {
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String INSURANCE_COMPANY_CODE_PREFIX = "LC";

    private final InsuranceCompanyRepository insuranceCompanyRepository;
    private final InsuranceProductRepository insuranceProductRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InsuranceManagementCompanyListItemResponse> getInsuranceCompanies(
            InsuranceManagementCompanyListRequest request
    ) {
        return PageResponse.from(
                insuranceCompanyRepository.searchManagementInsuranceCompanies(
                                request.normalizedInsuranceCompanyName(),
                                request.normalizedInsuranceCompanyStatus(),
                                request.toPageable()
                        )
                        .map(InsuranceManagementCompanyListItemResponse::from)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InsuranceManagementProductListItemResponse> getInsuranceProducts(
            InsuranceManagementProductListRequest request
    ) {
        return PageResponse.from(
                insuranceProductRepository.searchManagementInsuranceProducts(
                                request.getInsuranceCompanyId(),
                                request.getInsuranceCategoryId(),
                                request.normalizedInsuranceProductName(),
                                request.normalizedSaleStatus(),
                                LocalDate.now(),
                                request.toPageable()
                        )
                        .map(InsuranceManagementProductListItemResponse::from)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public InsuranceProductDetailResponse getInsuranceProductDetail(UUID insuranceProductId) {
        return insuranceProductRepository.findManagementInsuranceProductDetail(insuranceProductId)
                .map(InsuranceProductDetailResponse::from)
                .orElseThrow(() -> new BusinessException(InsuranceErrorCode.INSURANCE_PRODUCT_NOT_FOUND));
    }

    @Override
    @Transactional
    public InsuranceCompanyCreateResponse createInsuranceCompany(InsuranceCompanyCreateRequest request) {
        String insuranceCompanyName = request.getInsuranceCompanyName().trim();
        String insuranceCompanyCode = generateInsuranceCompanyCode();

        if (insuranceCompanyRepository.existsByInsuranceCompanyName(insuranceCompanyName)) {
            throw new BusinessException(InsuranceErrorCode.DUPLICATE_INSURANCE_COMPANY_NAME);
        }

        if (insuranceCompanyRepository.existsByInsuranceCompanyCode(insuranceCompanyCode)) {
            throw new BusinessException(InsuranceErrorCode.DUPLICATE_INSURANCE_COMPANY_CODE);
        }

        InsuranceCompany insuranceCompany = InsuranceCompany.builder()
                .id(UUID.randomUUID())
                .insuranceCompanyCode(insuranceCompanyCode)
                .insuranceCompanyName(insuranceCompanyName)
                .insuranceCompanyStatus(ACTIVE_STATUS)
                .insuranceCompanyPhone(request.getInsuranceCompanyPhone().trim())
                .deletedAt(null)
                .build();

        InsuranceCompany savedInsuranceCompany = insuranceCompanyRepository.save(insuranceCompany);

        return InsuranceCompanyCreateResponse.builder()
                .insuranceCompanyId(savedInsuranceCompany.getId())
                .insuranceCompanyCode(savedInsuranceCompany.getInsuranceCompanyCode())
                .insuranceCompanyName(savedInsuranceCompany.getInsuranceCompanyName())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InsuranceCompanyDetailResponse getInsuranceCompanyDetail(UUID insuranceCompanyId) {
        InsuranceCompany insuranceCompany = insuranceCompanyRepository.findById(insuranceCompanyId)
                .orElseThrow(() -> new BusinessException(InsuranceErrorCode.INSURANCE_COMPANY_NOT_FOUND));

        return InsuranceCompanyDetailResponse.from(insuranceCompany);
    }

    @Override
    @Transactional
    public InsuranceCompanyDetailResponse updateInsuranceCompany(
            UUID insuranceCompanyId,
            InsuranceCompanyUpdateRequest request
    ) {
        String insuranceCompanyName = request.getInsuranceCompanyName().trim();
        InsuranceCompany insuranceCompany = insuranceCompanyRepository.findById(insuranceCompanyId)
                .orElseThrow(() -> new BusinessException(InsuranceErrorCode.INSURANCE_COMPANY_NOT_FOUND));

        if (insuranceCompanyRepository.existsByInsuranceCompanyNameAndIdNot(insuranceCompanyName, insuranceCompanyId)) {
            throw new BusinessException(InsuranceErrorCode.DUPLICATE_INSURANCE_COMPANY_NAME);
        }

        insuranceCompany.update(
                insuranceCompanyName,
                request.getInsuranceCompanyPhone().trim(),
                request.getInsuranceCompanyStatus().trim()
        );

        return InsuranceCompanyDetailResponse.from(insuranceCompany);
    }

    private String generateInsuranceCompanyCode() {
        long nextSequence = insuranceCompanyRepository.findMaxInsuranceCompanyCodeSequence() + 1;
        return INSURANCE_COMPANY_CODE_PREFIX + String.format("%03d", nextSequence);
    }
}
