package com.linker.relia.consultation.service;

import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.consultation.domain.Consultation;
import com.linker.relia.consultation.domain.ConsultationCancelDetail;
import com.linker.relia.consultation.domain.ConsultationClaimDetail;
import com.linker.relia.consultation.domain.ConsultationClaimNextAction;
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
import com.linker.relia.consultation.repository.ConsultationClaimNextActionRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationServiceImpl implements ConsultationService {
    private static final String CUSTOMER_CODE_PREFIX = "CUS-";

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
    private final ConsultationClaimNextActionRepository consultationClaimNextActionRepository;
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

        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .customerCode(generateCustomerCode())
                .customerFp(fp)
                .customerStatus(CustomerStatus.PROSPECT)
                .customerGrade(CustomerGrade.GENERAL)
                .interestYn(false)
                .interestReason(null)
                .customerName(request.getCustomerName())
                .customerGender(request.getCustomerGender())
                .customerBirthDate(request.getCustomerBirthDate())
                .customerPhone(normalizedPhone)
                .customerEmail(request.getCustomerEmail())
                .customerZipcode(request.getCustomerZipcode())
                .customerAddressRoad(request.getCustomerAddressRoad())
                .customerAddressDetail(request.getCustomerAddressDetail())
                .customerJob(request.getCustomerJob())
                .customerCompanyName(request.getCustomerCompanyName())
                .customerAnnualIncome(request.getCustomerAnnualIncome())
                .customerAssetSize(request.getCustomerAssetSize())
                .customerDebtStatus(request.getCustomerDebtStatus())
                .customerIsSmoker(request.getCustomerIsSmoker())
                .customerIsDrinker(request.getCustomerIsDrinker())
                .customerMaritalStatus(request.getCustomerMaritalStatus())
                .customerDependentsCount(request.getCustomerDependentsCount())
                .build();

        Customer savedCustomer;
        try {
            savedCustomer = customerRepository.saveAndFlush(customer);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateCustomerPhoneViolation(e)) {
                throw new BusinessException(ConsultationErrorCode.DUPLICATE_CUSTOMER_PHONE);
            }
            throw e;
        }

        saveUnderlyingDiseases(savedCustomer, request.getUnderlyingDiseaseCodes());
        return savedCustomer;
    }

    private void saveUnderlyingDiseases(Customer customer, List<String> underlyingDiseaseCodes) {
        if (underlyingDiseaseCodes == null || underlyingDiseaseCodes.isEmpty()) {
            return;
        }

        Set<String> uniqueDiseaseCodes = new LinkedHashSet<>();
        for (String diseaseCode : underlyingDiseaseCodes) {
            if (diseaseCode == null || diseaseCode.trim().isEmpty()) {
                throw new IllegalArgumentException("기저질환 코드는 비어 있을 수 없습니다.");
            }

            String normalizedDiseaseCode = diseaseCode.trim();
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
        long nextSequence = customerRepository.getNextCustomerCodeSequence();
        return CUSTOMER_CODE_PREFIX + nextSequence;
    }

    private boolean isDuplicateCustomerPhoneViolation(DataIntegrityViolationException e) {
        Throwable current = e;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("uk_customers_phone")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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
        List<ConsultationClaimNextAction> nextActions =
                consultationClaimNextActionRepository
                        .findAllByConsultationClaimDetailIdOrderByActionOrderAsc(detail.getId());

        return ClaimDetailResponse.from(detail, claimTypes, reviewItems, nextActions);
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
            throw new IllegalArgumentException("신규 계약 상담 상세 정보는 필수입니다.");
        }

        ConsultationNewDetail detail = ConsultationNewDetail.builder()
                .consultation(consultation)
                .monthlyIncome(request.getNewDetail().getMonthlyIncome())
                .hasExistingInsurance(request.getNewDetail().getHasExistingInsurance())
                .monthlyInsurancePremium(request.getNewDetail().getMonthlyInsurancePremium())
                .existingInsuranceNote(request.getNewDetail().getExistingInsuranceNote())
                .insurancePriority(request.getNewDetail().getInsurancePriority())
                .build();

        consultationNewDetailRepository.save(detail);

        if (request.getNewDetail().getCoverageTypes() != null) {
            List<ConsultationNewCoverageNeed> coverageNeeds = request.getNewDetail().getCoverageTypes().stream()
                    .map(coverageType -> ConsultationNewCoverageNeed.builder()
                            .consultationNewDetail(detail)
                            .coverageType(coverageType.name())
                            .build())
                    .toList();

            consultationNewCoverageNeedRepository.saveAll(coverageNeeds);
        }

        if (request.getNewDetail().getProposedProductCodes() != null) {
            Set<String> requestedProductCodes = new LinkedHashSet<>(request.getNewDetail().getProposedProductCodes());
            Map<String, InsuranceProduct> productByCode = insuranceProductRepository
                    .findAllByInsuranceProductCodeInAndDeletedAtIsNull(requestedProductCodes)
                    .stream()
                    .collect(Collectors.toMap(
                            InsuranceProduct::getInsuranceProductCode,
                            Function.identity()
                    ));

            if (productByCode.size() != requestedProductCodes.size()) {
                throw new IllegalArgumentException("존재하지 않는 보험 상품입니다.");
            }

            List<ConsultationNewProposedProduct> proposedProducts = request.getNewDetail().getProposedProductCodes().stream()
                    .map(productCode -> {
                        InsuranceProduct product = productByCode.get(productCode);
                        return ConsultationNewProposedProduct.builder()
                                .consultationNewDetail(detail)
                                .insuranceProduct(product)
                                .insuranceProductName(product.getInsuranceProductName())
                                .build();
                    })
                    .toList();

            consultationNewProposedProductRepository.saveAll(proposedProducts);
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
                .claimStage("COMPLETED")
                .incidentDate(request.getClaimDetail().getIncidentDate())
                .claimResult(request.getClaimDetail().getResult())
                .claimReasonDetail(request.getClaimDetail().getClaimReason())
                .hospitalName(request.getClaimDetail().getHospitalName())
                .diagnosisOrTreatment(request.getClaimDetail().getDiagnosisOrTreatment())
                .hospitalizationStatus(request.getClaimDetail().getHospitalizationStatus())
                .surgeryStatus(request.getClaimDetail().getSurgeryStatus())
                .createdAt(now)
                .createdBy(fp.getId())
                .updatedAt(now)
                .updatedBy(fp.getId())
                .build();

        consultationClaimDetailRepository.save(detail);

        String claimType = request.getClaimDetail().getClaimType();
        if (claimType != null && !claimType.isBlank()) {
            consultationClaimTypeRepository.save(
                    ConsultationClaimType.builder()
                            .consultationClaimDetail(detail)
                            .claimType(claimType.trim())
                            .createdAt(now)
                            .createdBy(fp.getId())
                            .updatedAt(now)
                            .updatedBy(fp.getId())
                            .build()
            );
        }

        if (request.getClaimDetail().getReviewItems() != null) {
            List<ConsultationClaimReviewItem> reviewItems = request.getClaimDetail().getReviewItems().stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .distinct()
                    .map(value -> ConsultationClaimReviewItem.builder()
                            .consultationClaimDetail(detail)
                            .reviewType(value)
                            .createdAt(now)
                            .createdBy(fp.getId())
                            .updatedAt(now)
                            .updatedBy(fp.getId())
                            .build())
                    .toList();
            consultationClaimReviewItemRepository.saveAll(reviewItems);
        }

        if (request.getClaimDetail().getNextActions() != null) {
            List<ConsultationClaimNextAction> nextActions = new java.util.ArrayList<>();
            for (String value : request.getClaimDetail().getNextActions()) {
                if (value == null || value.isBlank()) {
                    continue;
                }
                nextActions.add(ConsultationClaimNextAction.builder()
                        .consultationClaimDetail(detail)
                        .actionOrder(nextActions.size() + 1)
                        .actionContent(value.trim())
                        .createdAt(now)
                        .createdBy(fp.getId())
                        .updatedAt(now)
                        .updatedBy(fp.getId())
                        .build());
            }
            consultationClaimNextActionRepository.saveAll(nextActions);
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
                .nextActions(request.getRenewalDetail().getNextActions())
                .decisionExpectedDate(request.getRenewalDetail().getDecisionExpectedDate())
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
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("휴대폰 번호는 비어 있을 수 없습니다.");
        }

        return value.trim()
                .replace("-", "")
                .replace(" ", "");
    }

}
