package com.linker.relia.insurance.service;

import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.common.exception.CommonErrorCode;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.insurance.domain.InsuranceCompany;
import com.linker.relia.insurance.domain.InsuranceCategory;
import com.linker.relia.insurance.domain.InsuranceProduct;
import com.linker.relia.insurance.dto.request.InsuranceCompanyCreateRequest;
import com.linker.relia.insurance.dto.request.InsuranceCompanyUpdateRequest;
import com.linker.relia.insurance.dto.request.InsuranceManagementCompanyListRequest;
import com.linker.relia.insurance.dto.request.InsuranceManagementProductListRequest;
import com.linker.relia.insurance.dto.request.InsuranceProductCreateRequest;
import com.linker.relia.insurance.dto.request.InsuranceProductUpdateRequest;
import com.linker.relia.insurance.dto.response.InsuranceCompanyCreateResponse;
import com.linker.relia.insurance.dto.response.InsuranceCompanyDetailResponse;
import com.linker.relia.insurance.dto.response.InsuranceManagementCompanyListItemResponse;
import com.linker.relia.insurance.dto.response.InsuranceManagementProductListItemResponse;
import com.linker.relia.insurance.dto.response.InsuranceProductDetailResponse;
import com.linker.relia.insurance.exception.InsuranceErrorCode;
import com.linker.relia.insurance.repository.InsuranceCompanyRepository;
import com.linker.relia.insurance.repository.InsuranceCategoryRepository;
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
    private static final String INSURANCE_PRODUCT_CODE_PREFIX = "LP";

    private final InsuranceCompanyRepository insuranceCompanyRepository;
    private final InsuranceCategoryRepository insuranceCategoryRepository;
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
    public InsuranceProductDetailResponse createInsuranceProduct(InsuranceProductCreateRequest request) {
        String insuranceProductName = request.normalizedInsuranceProductName();
        String insuranceProductStatus = ACTIVE_STATUS;
        String coveragePeriodType = request.normalizedCoveragePeriodType();
        String insuranceProductCode = generateInsuranceProductCode();

        if (insuranceProductRepository.existsByInsuranceProductCode(insuranceProductCode)) {
            throw new BusinessException(InsuranceErrorCode.DUPLICATE_INSURANCE_PRODUCT_CODE);
        }

        InsuranceCompany insuranceCompany = insuranceCompanyRepository
                .findByIdAndInsuranceCompanyStatusAndDeletedAtIsNull(request.getInsuranceCompanyId(), ACTIVE_STATUS)
                .orElseThrow(() -> new BusinessException(InsuranceErrorCode.INSURANCE_COMPANY_NOT_FOUND));

        InsuranceCategory insuranceCategory = insuranceCategoryRepository
                .findByIdAndInsuranceCategoryStatusAndDeletedAtIsNull(request.getInsuranceCategoryId(), ACTIVE_STATUS)
                .orElseThrow(() -> new BusinessException(InsuranceErrorCode.INSURANCE_CATEGORY_NOT_FOUND));

        validateInsuranceProductDates(request);

        Integer coveragePeriodYears = resolveCoveragePeriodYears(request, coveragePeriodType);
        Integer coverageAgeLimit = resolveCoverageAgeLimit(request, coveragePeriodType);
        Boolean isLifetimeCoverage = resolveLifetimeCoverage(coveragePeriodType);
        Integer renewalCycle = resolveRenewalCycle(request);

        InsuranceProduct insuranceProduct = InsuranceProduct.builder()
                .id(UUID.randomUUID())
                .insuranceProductCode(insuranceProductCode)
                .insuranceCompany(insuranceCompany)
                .insuranceCategory(insuranceCategory)
                .insuranceProductName(insuranceProductName)
                .insuranceProductStatus(insuranceProductStatus)
                .insuranceStartDate(request.getInsuranceStartDate())
                .insuranceEndDate(request.getInsuranceEndDate())
                .coveragePeriodType(coveragePeriodType)
                .coveragePeriodYears(coveragePeriodYears)
                .coverageAgeLimit(coverageAgeLimit)
                .isLifetimeCoverage(isLifetimeCoverage)
                .isRenewable(request.getIsRenewable())
                .renewalCycle(renewalCycle)
                .productDescription(request.normalizedProductDescription())
                .deletedAt("INACTIVE".equals(insuranceProductStatus) ? java.time.LocalDateTime.now() : null)
                .build();

        InsuranceProduct savedInsuranceProduct = insuranceProductRepository.save(insuranceProduct);
        return InsuranceProductDetailResponse.from(savedInsuranceProduct);
    }

    @Override
    @Transactional
    public InsuranceProductDetailResponse updateInsuranceProduct(
            UUID insuranceProductId,
            InsuranceProductUpdateRequest request
    ) {
        InsuranceProduct insuranceProduct = insuranceProductRepository.findManagementInsuranceProductDetail(insuranceProductId)
                .orElseThrow(() -> new BusinessException(InsuranceErrorCode.INSURANCE_PRODUCT_NOT_FOUND));

        insuranceProduct.updateManagementInfo(
                request.normalizedInsuranceProductStatus(),
                request.normalizedProductDescription()
        );

        return InsuranceProductDetailResponse.from(insuranceProduct);
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

    private String generateInsuranceProductCode() {
        long nextSequence = insuranceProductRepository.findMaxInsuranceProductCodeSequence() + 1;
        return INSURANCE_PRODUCT_CODE_PREFIX + String.format("%03d", nextSequence);
    }

    private void validateInsuranceProductDates(InsuranceProductCreateRequest request) {
        if (request.getInsuranceEndDate() != null && request.getInsuranceEndDate().isBefore(request.getInsuranceStartDate())) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "판매 종료일은 출시일보다 빠를 수 없습니다.");
        }
    }

    private Integer resolveCoveragePeriodYears(InsuranceProductCreateRequest request, String coveragePeriodType) {
        return switch (coveragePeriodType) {
            case "YEARS" -> {
                if (request.getCoveragePeriodYears() == null || request.getCoveragePeriodYears() <= 0) {
                    throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "연단위 보장기간은 1년 이상 입력해야 합니다.");
                }
                yield request.getCoveragePeriodYears();
            }
            case "AGE", "LIFETIME" -> null;
            default -> throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "유효하지 않은 보장기간 유형입니다.");
        };
    }

    private Integer resolveCoverageAgeLimit(InsuranceProductCreateRequest request, String coveragePeriodType) {
        return switch (coveragePeriodType) {
            case "AGE" -> {
                if (request.getCoverageAgeLimit() == null || request.getCoverageAgeLimit() <= 0) {
                    throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "나이 기준 보장기간은 최대 보장 나이를 입력해야 합니다.");
                }
                yield request.getCoverageAgeLimit();
            }
            case "YEARS", "LIFETIME" -> null;
            default -> throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "유효하지 않은 보장기간 유형입니다.");
        };
    }

    private Boolean resolveLifetimeCoverage(String coveragePeriodType) {
        return "LIFETIME".equals(coveragePeriodType);
    }

    private Integer resolveRenewalCycle(InsuranceProductCreateRequest request) {
        if (Boolean.FALSE.equals(request.getIsRenewable())) {
            return null;
        }

        if (request.getRenewalCycle() == null || request.getRenewalCycle() <= 0) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "갱신형 상품은 갱신 주기를 입력해야 합니다.");
        }

        return request.getRenewalCycle();
    }
}
