package com.linker.relia.consultation.service;

import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.consultation.domain.Consultation;
import com.linker.relia.consultation.domain.ConsultationCancelDetail;
import com.linker.relia.consultation.domain.ConsultationClaimDetail;
import com.linker.relia.consultation.domain.ConsultationClaimReviewItem;
import com.linker.relia.consultation.domain.ConsultationClaimType;
import com.linker.relia.consultation.domain.ConsultationNewCoverageNeed;
import com.linker.relia.consultation.domain.ConsultationNewDetail;
import com.linker.relia.consultation.domain.ConsultationNewProposedProduct;
import com.linker.relia.consultation.domain.ConsultationRenewalDetail;
import com.linker.relia.consultation.domain.ConsultationRenewalInterest;
import com.linker.relia.consultation.domain.ConsultationRenewalPremiumChangeReason;
import com.linker.relia.consultation.domain.ConsultationType;
import com.linker.relia.consultation.dto.request.ConsultationCreateRequest;
import com.linker.relia.consultation.dto.request.CustomerInfoRequest;
import com.linker.relia.consultation.dto.response.CancelDetailResponse;
import com.linker.relia.consultation.dto.response.ClaimDetailResponse;
import com.linker.relia.consultation.dto.response.ConsultationCreateResponse;
import com.linker.relia.consultation.dto.response.ConsultationDetailResponse;
import com.linker.relia.consultation.dto.response.ConsultationListResponse;
import com.linker.relia.consultation.dto.response.NewDetailResponse;
import com.linker.relia.consultation.dto.response.RenewalDetailResponse;
import com.linker.relia.consultation.exception.ConsultationErrorCode;
import com.linker.relia.consultation.repository.ConsultationCancelDetailRepository;
import com.linker.relia.consultation.repository.ConsultationClaimDetailRepository;
import com.linker.relia.consultation.repository.ConsultationClaimReviewItemRepository;
import com.linker.relia.consultation.repository.ConsultationClaimTypeRepository;
import com.linker.relia.consultation.repository.ConsultationNewCoverageNeedRepository;
import com.linker.relia.consultation.repository.ConsultationNewDetailRepository;
import com.linker.relia.consultation.repository.ConsultationNewProposedProductRepository;
import com.linker.relia.consultation.repository.ConsultationRenewalDetailRepository;
import com.linker.relia.consultation.repository.ConsultationRenewalInterestRepository;
import com.linker.relia.consultation.repository.ConsultationRenewalPremiumChangeReasonRepository;
import com.linker.relia.consultation.repository.ConsultationRepository;
import com.linker.relia.contract.domain.Contract;
import com.linker.relia.contract.repository.ContractRepository;
import com.linker.relia.customer.domain.Customer;
import com.linker.relia.customer.domain.CustomerGrade;
import com.linker.relia.customer.domain.CustomerStatus;
import com.linker.relia.customer.domain.CustomerUnderlyingDisease;
import com.linker.relia.customer.exception.CustomerErrorCode;
import com.linker.relia.customer.repository.CustomerRepository;
import com.linker.relia.customer.repository.CustomerUnderlyingDiseaseRepository;
import com.linker.relia.customer.repository.DiseaseCodeRepository;
import com.linker.relia.insurance.domain.InsuranceProduct;
import com.linker.relia.insurance.repository.InsuranceProductRepository;
import com.linker.relia.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationServiceImpl implements ConsultationService {
    private static final String CUSTOMER_CODE_PREFIX = "CUS";

    private final ConsultationRepository consultationRepository;
    private final CustomerRepository customerRepository;
    private final ContractRepository contractRepository;
    private final DiseaseCodeRepository diseaseCodeRepository;
    private final CustomerUnderlyingDiseaseRepository customerUnderlyingDiseaseRepository;

    private final ConsultationNewDetailRepository consultationNewDetailRepository;
    private final ConsultationClaimDetailRepository consultationClaimDetailRepository;
    private final ConsultationRenewalDetailRepository consultationRenewalDetailRepository;
    private final ConsultationCancelDetailRepository consultationCancelDetailRepository;

    private final ConsultationNewCoverageNeedRepository consultationNewCoverageNeedRepository;
    private final ConsultationNewProposedProductRepository consultationNewProposedProductRepository;
    private final ConsultationClaimTypeRepository consultationClaimTypeRepository;
    private final ConsultationClaimReviewItemRepository consultationClaimReviewItemRepository;
    private final ConsultationRenewalPremiumChangeReasonRepository consultationRenewalPremiumChangeReasonRepository;
    private final ConsultationRenewalInterestRepository consultationRenewalInterestRepository;

    private final InsuranceProductRepository insuranceProductRepository;

    @Override
    public ConsultationCreateResponse createConsultation(ConsultationCreateRequest request, User fp) {
        Customer customer = resolveCustomer(request, fp);
        Contract contract = resolveContract(request);

        int nextSequence = consultationRepository
                .findMaxSequenceByCustomerId(customer.getId())
                .orElse(0) + 1;

        LocalDateTime now = LocalDateTime.now();

        Consultation consultation = Consultation.builder()
                .consultationSequence(nextSequence)
                .customer(customer)
                .fp(fp)
                .contract(contract)
                .consultationType(request.getConsultationType())
                .consultationChannel(request.getConsultationChannel())
                .consultedAt(request.getConsultedAt())
                .specialNote(request.getSpecialNote())
                .nextScheduledAt(request.getNextScheduledAt())
                .build();

        consultationRepository.save(consultation);
        saveConsultationDetail(request, consultation, fp, now);

        return new ConsultationCreateResponse(consultation.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConsultationListResponse> getConsultations(Pageable pageable) {
        return consultationRepository.findAllByDeletedAtIsNull(pageable)
                .map(ConsultationListResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationDetailResponse getConsultationDetail(UUID consultationId, User fp) {
        Consultation consultation = consultationRepository
                .findByIdAndDeletedAtIsNull(consultationId)
                .orElseThrow(() -> new BusinessException(ConsultationErrorCode.CONSULTATION_NOT_FOUND));
        validateConsultationAccess(consultation, fp);

        NewDetailResponse newDetail = null;
        RenewalDetailResponse renewalDetail = null;
        ClaimDetailResponse claimDetail = null;
        CancelDetailResponse cancelDetail = null;

        if (consultation.getConsultationType() == ConsultationType.NEW_CONTRACT) {
            newDetail = getNewDetailResponse(consultationId);
        }

        if (consultation.getConsultationType() == ConsultationType.RENEWAL) {
            renewalDetail = getRenewalDetailResponse(consultationId);
        }

        if (consultation.getConsultationType() == ConsultationType.CLAIM) {
            claimDetail = getClaimDetailResponse(consultationId);
        }

        if (consultation.getConsultationType() == ConsultationType.TERMINATION) {
            cancelDetail = getCancelDetailResponse(consultationId);
        }

        return ConsultationDetailResponse.from(
                consultation,
                newDetail,
                renewalDetail,
                claimDetail,
                cancelDetail
        );
    }

    private Customer resolveCustomer(ConsultationCreateRequest request, User fp) {
        if (request.getCustomerId() != null && request.getCustomerInfo() != null) {
            throw new BusinessException(ConsultationErrorCode.CUSTOMER_TARGET_CONFLICT);
        }

        if (request.getConsultationType() == ConsultationType.NEW_CONTRACT) {
            if (request.getCustomerId() == null && request.getCustomerInfo() == null) {
                throw new BusinessException(ConsultationErrorCode.CUSTOMER_TARGET_REQUIRED);
            }

            if (request.getCustomerInfo() != null) {
                return createProspectCustomer(request.getCustomerInfo(), fp);
            }
        } else {
            if (request.getCustomerId() == null) {
                throw new BusinessException(ConsultationErrorCode.CUSTOMER_ID_REQUIRED);
            }

            if (request.getCustomerInfo() != null) {
                throw new BusinessException(ConsultationErrorCode.CUSTOMER_INFO_NOT_ALLOWED);
            }
        }

        return customerRepository.findByIdAndDeletedAtIsNull(request.getCustomerId())
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUSTOMER_NOT_FOUND));
    }

    private Contract resolveContract(ConsultationCreateRequest request) {
        if (request.getConsultationType() == ConsultationType.NEW_CONTRACT) {
            if (request.getContractId() != null) {
                throw new BusinessException(ConsultationErrorCode.CONTRACT_NOT_ALLOWED);
            }
            return null;
        }

        if (request.getContractId() == null) {
            throw new BusinessException(ConsultationErrorCode.CONTRACT_REQUIRED);
        }

        return contractRepository.findById(request.getContractId())
                .orElseThrow(() -> new BusinessException(ConsultationErrorCode.CONTRACT_NOT_FOUND));
    }

    private Customer createProspectCustomer(CustomerInfoRequest request, User fp) {
        String normalizedPhone = normalizePhone(request.getCustomerPhone());
        if (customerRepository.countActiveCustomerByNormalizedPhone(normalizedPhone) > 0) {
            throw new BusinessException(ConsultationErrorCode.DUPLICATE_CUSTOMER_PHONE);
        }

        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .customerCode(generateCustomerCode())
                .customerFp(fp)
                .customerStatus(CustomerStatus.PROSPECT)
                .customerGrade(CustomerGrade.GENERAL)
                .interestYn(false)
                .interestReason(null)
                .customerName(normalizeRequired(request.getCustomerName()))
                .customerGender(normalizeRequired(request.getCustomerGender()))
                .customerBirthDate(request.getCustomerBirthDate())
                .customerPhone(normalizeRequired(request.getCustomerPhone()))
                .customerEmail(normalizeRequired(request.getCustomerEmail()))
                .customerZipcode(normalizeRequired(request.getCustomerZipcode()))
                .customerAddressRoad(normalizeRequired(request.getCustomerAddressRoad()))
                .customerAddressDetail(normalizeNullable(request.getCustomerAddressDetail()))
                .customerJob(normalizeRequired(request.getCustomerJob()))
                .customerCompanyName(normalizeRequired(request.getCustomerCompanyName()))
                .customerAnnualIncome(request.getCustomerAnnualIncome())
                .customerAssetSize(request.getCustomerAssetSize())
                .customerDebtStatus(normalizeRequired(request.getCustomerDebtStatus()))
                .customerIsSmoker(request.getCustomerIsSmoker())
                .customerIsDrinker(request.getCustomerIsDrinker())
                .customerMaritalStatus(request.getCustomerMaritalStatus())
                .customerDependentsCount(request.getCustomerDependentsCount())
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        saveUnderlyingDiseases(savedCustomer, request.getUnderlyingDiseaseCodes());
        return savedCustomer;
    }

    private void saveUnderlyingDiseases(Customer customer, List<String> underlyingDiseaseCodes) {
        if (underlyingDiseaseCodes == null || underlyingDiseaseCodes.isEmpty()) {
            return;
        }

        Set<String> uniqueDiseaseCodes = new LinkedHashSet<>();
        for (String diseaseCode : underlyingDiseaseCodes) {
            String normalizedDiseaseCode = normalizeRequired(diseaseCode);
            if (!diseaseCodeRepository.existsByDiseaseCodeAndDeletedAtIsNull(normalizedDiseaseCode)) {
                throw new BusinessException(ConsultationErrorCode.INVALID_DISEASE_CODE);
            }
            uniqueDiseaseCodes.add(normalizedDiseaseCode);
        }

        for (String diseaseCode : uniqueDiseaseCodes) {
            customerUnderlyingDiseaseRepository.save(
                    CustomerUnderlyingDisease.builder()
                            .id(UUID.randomUUID())
                            .customer(customer)
                            .diseaseCode(diseaseCode)
                            .build()
            );
        }
    }

    private String generateCustomerCode() {
        long nextSequence = customerRepository.findMaxCustomerCodeSequence() + 1;
        return CUSTOMER_CODE_PREFIX + String.format("%06d", nextSequence);
    }

    private NewDetailResponse getNewDetailResponse(UUID consultationId) {
        ConsultationNewDetail detail = consultationNewDetailRepository
                .findByConsultationId(consultationId)
                .orElse(null);

        if (detail == null) {
            return null;
        }

        List<ConsultationNewCoverageNeed> coverageNeeds =
                consultationNewCoverageNeedRepository.findAllByConsultationNewDetailId(detail.getId());

        List<ConsultationNewProposedProduct> proposedProducts =
                consultationNewProposedProductRepository.findAllByConsultationNewDetailId(detail.getId());

        return NewDetailResponse.from(detail, coverageNeeds, proposedProducts);
    }

    private void validateConsultationAccess(Consultation consultation, User fp) {
        switch (fp.getUserRole()) {
            case HQ_MANAGER -> {
                return;
            }
            case BRANCH_MANAGER -> {
                if (!consultation.getCustomer().getCustomerFp().getOrganization().getId()
                        .equals(fp.getOrganization().getId())) {
                    throw new BusinessException(ConsultationErrorCode.CONSULTATION_ACCESS_DENIED);
                }
            }
            case FP -> {
                if (!consultation.getCustomer().getCustomerFp().getId().equals(fp.getId())) {
                    throw new BusinessException(ConsultationErrorCode.CONSULTATION_ACCESS_DENIED);
                }
            }
            default -> throw new BusinessException(ConsultationErrorCode.CONSULTATION_ACCESS_DENIED);
        }
    }

    private RenewalDetailResponse getRenewalDetailResponse(UUID consultationId) {
        ConsultationRenewalDetail detail =
                consultationRenewalDetailRepository.findByConsultationId(consultationId).orElse(null);

        if (detail == null) {
            return null;
        }

        List<ConsultationRenewalPremiumChangeReason> premiumChangeReasons =
                consultationRenewalPremiumChangeReasonRepository.findAllByConsultationRenewalDetailId(detail.getId());

        List<ConsultationRenewalInterest> interests =
                consultationRenewalInterestRepository.findAllByConsultationRenewalDetailId(detail.getId());

        return RenewalDetailResponse.from(detail, premiumChangeReasons, interests);
    }

    private ClaimDetailResponse getClaimDetailResponse(UUID consultationId) {
        ConsultationClaimDetail detail =
                consultationClaimDetailRepository.findByConsultationId(consultationId).orElse(null);

        if (detail == null) {
            return null;
        }

        List<ConsultationClaimType> claimTypes =
                consultationClaimTypeRepository.findAllByConsultationClaimDetailId(detail.getId());

        List<ConsultationClaimReviewItem> reviewItems =
                consultationClaimReviewItemRepository.findAllByConsultationClaimDetailId(detail.getId());

        return ClaimDetailResponse.from(detail, claimTypes, reviewItems);
    }

    private CancelDetailResponse getCancelDetailResponse(UUID consultationId) {
        ConsultationCancelDetail detail =
                consultationCancelDetailRepository.findByConsultationId(consultationId).orElse(null);

        if (detail == null) {
            return null;
        }

        return CancelDetailResponse.from(detail);
    }

    private void saveConsultationDetail(
            ConsultationCreateRequest request,
            Consultation consultation,
            User fp,
            LocalDateTime now
    ) {
        switch (request.getConsultationType()) {
            case NEW_CONTRACT -> saveNewContractDetail(request, consultation, fp, now);
            case CLAIM -> saveClaimDetail(request, consultation, fp, now);
            case RENEWAL -> saveRenewalDetail(request, consultation, fp, now);
            case TERMINATION -> saveCancelDetail(request, consultation, fp, now);
        }
    }

    private void saveNewContractDetail(
            ConsultationCreateRequest request,
            Consultation consultation,
            User fp,
            LocalDateTime now
    ) {
        if (request.getNewDetail() == null) {
            throw new IllegalArgumentException("신규 상담 상세 정보는 필수입니다.");
        }

        ConsultationNewDetail detail = ConsultationNewDetail.builder()
                .consultation(consultation)
                .monthlyIncome(request.getNewDetail().getMonthlyIncome())
                .hasExistingInsurance(request.getNewDetail().getHasExistingInsurance())
                .monthlyInsurancePremium(request.getNewDetail().getMonthlyInsurancePremium())
                .existingInsuranceNote(request.getNewDetail().getExistingInsuranceNote())
                .insurancePriority(request.getNewDetail().getInsurancePriority())
                .createdAt(now)
                .createdBy(fp.getId())
                .updatedAt(now)
                .updatedBy(fp.getId())
                .build();

        consultationNewDetailRepository.save(detail);

        if (request.getNewDetail().getCoverageTypes() != null) {
            for (String coverageType : request.getNewDetail().getCoverageTypes()) {
                consultationNewCoverageNeedRepository.save(
                        ConsultationNewCoverageNeed.builder()
                                .consultationNewDetail(detail)
                                .coverageType(coverageType)
                                .createdAt(now)
                                .createdBy(fp.getId())
                                .updatedAt(now)
                                .updatedBy(fp.getId())
                                .build()
                );
            }
        }

        if (request.getNewDetail().getProposedProductCodes() != null) {
            for (String productCode : request.getNewDetail().getProposedProductCodes()) {
                InsuranceProduct product = insuranceProductRepository
                        .findByInsuranceProductCodeAndDeletedAtIsNull(productCode)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 보험 상품입니다."));

                consultationNewProposedProductRepository.save(
                        ConsultationNewProposedProduct.builder()
                                .consultationNewDetail(detail)
                                .insuranceProduct(product)
                                .insuranceProductName(product.getInsuranceProductName())
                                .createdAt(now)
                                .createdBy(fp.getId())
                                .updatedAt(now)
                                .updatedBy(fp.getId())
                                .build()
                );
            }
        }
    }

    private void saveClaimDetail(
            ConsultationCreateRequest request,
            Consultation consultation,
            User fp,
            LocalDateTime now
    ) {
        if (request.getClaimDetail() == null) {
            throw new IllegalArgumentException("보험금 청구 상담 상세 정보는 필수입니다.");
        }

        ConsultationClaimDetail detail = ConsultationClaimDetail.builder()
                .consultation(consultation)
                .claimStage(request.getClaimDetail().getClaimStage())
                .claimEventDate(request.getClaimDetail().getClaimEventDate())
                .claimReasonDetail(request.getClaimDetail().getClaimReasonDetail())
                .hospitalName(request.getClaimDetail().getHospitalName())
                .diagnosisOrTreatment(request.getClaimDetail().getDiagnosisOrTreatment())
                .hospitalizationStatus(request.getClaimDetail().getHospitalizationStatus())
                .surgeryStatus(request.getClaimDetail().getSurgeryStatus())
                .claimResult(request.getClaimDetail().getClaimResult())
                .guidanceSummary(request.getClaimDetail().getGuidanceSummary())
                .createdAt(now)
                .createdBy(fp.getId())
                .updatedAt(now)
                .updatedBy(fp.getId())
                .build();

        consultationClaimDetailRepository.save(detail);

        if (request.getClaimDetail().getClaimTypes() != null) {
            for (String claimType : request.getClaimDetail().getClaimTypes()) {
                consultationClaimTypeRepository.save(
                        ConsultationClaimType.builder()
                                .consultationClaimDetail(detail)
                                .claimType(claimType)
                                .createdAt(now)
                                .createdBy(fp.getId())
                                .updatedAt(now)
                                .updatedBy(fp.getId())
                                .build()
                );
            }
        }

        if (request.getClaimDetail().getReviewTypes() != null) {
            for (String reviewType : request.getClaimDetail().getReviewTypes()) {
                consultationClaimReviewItemRepository.save(
                        ConsultationClaimReviewItem.builder()
                                .consultationClaimDetail(detail)
                                .reviewType(reviewType)
                                .createdAt(now)
                                .createdBy(fp.getId())
                                .updatedAt(now)
                                .updatedBy(fp.getId())
                                .build()
                );
            }
        }
    }

    private void saveRenewalDetail(
            ConsultationCreateRequest request,
            Consultation consultation,
            User fp,
            LocalDateTime now
    ) {
        if (request.getRenewalDetail() == null) {
            throw new IllegalArgumentException("갱신 상담 상세 정보는 필수입니다.");
        }

        ConsultationRenewalDetail detail = ConsultationRenewalDetail.builder()
                .consultation(consultation)
                .renewalReason(request.getRenewalDetail().getRenewalReason())
                .renewalScheduledDate(request.getRenewalDetail().getRenewalScheduledDate())
                .currentPremium(request.getRenewalDetail().getCurrentPremium())
                .renewalPremium(request.getRenewalDetail().getRenewalPremium())
                .premiumChangeRate(request.getRenewalDetail().getPremiumChangeRate())
                .coverageChangeType(request.getRenewalDetail().getCoverageChangeType())
                .coverageChangeDetail(request.getRenewalDetail().getCoverageChangeDetail())
                .customerReaction(request.getRenewalDetail().getCustomerReaction())
                .consultationResult(request.getRenewalDetail().getConsultationResult())
                .createdAt(now)
                .createdBy(fp.getId())
                .updatedAt(now)
                .updatedBy(fp.getId())
                .build();

        consultationRenewalDetailRepository.save(detail);

        if (request.getRenewalDetail().getPremiumChangeReasonTypes() != null) {
            for (String reasonType : request.getRenewalDetail().getPremiumChangeReasonTypes()) {
                consultationRenewalPremiumChangeReasonRepository.save(
                        ConsultationRenewalPremiumChangeReason.builder()
                                .consultationRenewalDetail(detail)
                                .reasonType(reasonType)
                                .otherReason(
                                        "OTHER".equals(reasonType)
                                                ? request.getRenewalDetail().getOtherReason()
                                                : null
                                )
                                .createdAt(now)
                                .createdBy(fp.getId())
                                .updatedAt(now)
                                .updatedBy(fp.getId())
                                .build()
                );
            }
        }

        if (request.getRenewalDetail().getInterestTypes() != null) {
            for (String interestType : request.getRenewalDetail().getInterestTypes()) {
                consultationRenewalInterestRepository.save(
                        ConsultationRenewalInterest.builder()
                                .consultationRenewalDetail(detail)
                                .interestType(interestType)
                                .createdAt(now)
                                .createdBy(fp.getId())
                                .updatedAt(now)
                                .updatedBy(fp.getId())
                                .build()
                );
            }
        }
    }

    private void saveCancelDetail(
            ConsultationCreateRequest request,
            Consultation consultation,
            User fp,
            LocalDateTime now
    ) {
        if (request.getCancelDetail() == null) {
            throw new IllegalArgumentException("해지 상담 상세 정보는 필수입니다.");
        }

        ConsultationCancelDetail detail = ConsultationCancelDetail.builder()
                .consultation(consultation)
                .premiumBurden(request.getCancelDetail().getPremiumBurden())
                .renewalPremiumBurden(request.getCancelDetail().getRenewalPremiumBurden())
                .paymentDifficulty(request.getCancelDetail().getPaymentDifficulty())
                .coverageDissatisfaction(request.getCancelDetail().getCoverageDissatisfaction())
                .duplicateInsurance(request.getCancelDetail().getDuplicateInsurance())
                .productRemodelingReview(request.getCancelDetail().getProductRemodelingReview())
                .comparingOtherCompany(request.getCancelDetail().getComparingOtherCompany())
                .movingToOtherCompany(request.getCancelDetail().getMovingToOtherCompany())
                .plannerContactDissatisfaction(request.getCancelDetail().getPlannerContactDissatisfaction())
                .managementDissatisfaction(request.getCancelDetail().getManagementDissatisfaction())
                .retentionPossibility(request.getCancelDetail().getRetentionPossibility())
                .createdAt(now)
                .createdBy(fp.getId())
                .updatedAt(now)
                .updatedBy(fp.getId())
                .build();

        consultationCancelDetailRepository.save(detail);
    }

    private String normalizePhone(String value) {
        return normalizeRequired(value)
                .replace("-", "")
                .replace(" ", "");
    }

    private String normalizeRequired(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new IllegalArgumentException("필수 문자열 값이 비어 있습니다.");
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
