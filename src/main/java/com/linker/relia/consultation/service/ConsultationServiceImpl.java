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
import com.linker.relia.customer.repository.CustomerRepository;
import com.linker.relia.insurance.domain.InsuranceProduct;
import com.linker.relia.insurance.repository.InsuranceProductRepository;
import com.linker.relia.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationServiceImpl implements ConsultationService {
    private final ConsultationRepository consultationRepository;
    private final CustomerRepository customerRepository;
    private final ContractRepository contractRepository;

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
    public ConsultationCreateResponse createConsultation(
            ConsultationCreateRequest request,
            User fp
    ) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("고객이 존재하지 않습니다."));

        Contract contract = null;
        if (request.getConsultationType() == ConsultationType.NEW_CONTRACT) {

            if (request.getContractId() != null) {
                throw new BusinessException(
                        ConsultationErrorCode.CONTRACT_NOT_ALLOWED
                );
            }

        } else {

            if (request.getContractId() == null) {
                throw new BusinessException(
                        ConsultationErrorCode.CONTRACT_REQUIRED
                );
            }

            contract = contractRepository.findById(request.getContractId())
                    .orElseThrow(() ->
                            new BusinessException(
                                    ConsultationErrorCode.CONTRACT_NOT_FOUND
                            )
                    );
        }

        int nextSequence = consultationRepository
                .findMaxSequenceByCustomerId(request.getCustomerId())
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

        return new ConsultationCreateResponse(
                consultation.getId()
        );

    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConsultationListResponse> getConsultations(Pageable pageable){
        return consultationRepository.findAllByDeletedAtIsNull(pageable)
                .map(ConsultationListResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationDetailResponse getConsultationDetail(
            UUID consultationId,
            User fp
    ){
        Consultation consultation = consultationRepository
                .findByIdAndDeletedAtIsNull(consultationId)
                .orElseThrow(() ->
                        new BusinessException(ConsultationErrorCode.CONSULTATION_NOT_FOUND)
                );
        NewDetailResponse newDetail = null;
        RenewalDetailResponse renewalDetail = null;

        if (consultation.getConsultationType() == ConsultationType.NEW_CONTRACT) {
            newDetail = getNewDetailResponse(consultationId);
        }

        if (consultation.getConsultationType() == ConsultationType.RENEWAL) {
            renewalDetail = getRenewalDetailResponse(consultationId);
        }

        return ConsultationDetailResponse.from(
                consultation,
                newDetail,
                renewalDetail
        );
    }

    private NewDetailResponse getNewDetailResponse(UUID consultationId) {
        ConsultationNewDetail detail = consultationNewDetailRepository
                .findByConsultationId(consultationId)
                .orElse(null);

        if (detail == null) {
            return null;
        }

        List<ConsultationNewCoverageNeed> coverageNeeds =
                consultationNewCoverageNeedRepository
                        .findAllByConsultationNewDetailId(detail.getId());

        List<ConsultationNewProposedProduct> proposedProducts =
                consultationNewProposedProductRepository
                        .findAllByConsultationNewDetailId(detail.getId());

        return NewDetailResponse.from(
                detail,
                coverageNeeds,
                proposedProducts
        );
    }

    private RenewalDetailResponse getRenewalDetailResponse(UUID consultationId) {

        ConsultationRenewalDetail detail =
                consultationRenewalDetailRepository
                        .findByConsultationId(consultationId)
                        .orElse(null);

        if (detail == null) {
            return null;
        }

        List<ConsultationRenewalPremiumChangeReason> premiumChangeReasons =
                consultationRenewalPremiumChangeReasonRepository
                        .findAllByConsultationRenewalDetailId(detail.getId());

        List<ConsultationRenewalInterest> interests =
                consultationRenewalInterestRepository
                        .findAllByConsultationRenewalDetailId(detail.getId());

        return RenewalDetailResponse.from(
                detail,
                premiumChangeReasons,
                interests
        );
    }

    private void saveConsultationDetail(
            ConsultationCreateRequest request,
            Consultation consultation,
            User fp,
            LocalDateTime now
    ){
        switch (request.getConsultationType()){
            case NEW_CONTRACT -> {
                saveNewContractDetail(request, consultation, fp, now);
            }
            case CLAIM -> {
                saveClaimDetail(request, consultation, fp, now);
            }
            case RENEWAL -> {
                saveRenewalDetail(request, consultation, fp, now);
            }
            case TERMINATION -> {
                saveCancelDetail(request, consultation, fp, now);
            }
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

        if(request.getNewDetail().getProposedProductIds() != null){
            for(UUID productId : request.getNewDetail().getProposedProductIds()){
                InsuranceProduct product = insuranceProductRepository.findById(productId)
                        .orElseThrow(() -> new IllegalArgumentException("보험 상품이 존재하지 않습니다."));

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
        if(request.getClaimDetail() == null){
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

        if (request.getClaimDetail().getClaimTypes() != null){
           for(String claimType : request.getClaimDetail().getClaimTypes()){
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

        if(request.getClaimDetail().getReviewTypes() != null){
            for(String reviewType : request.getClaimDetail().getReviewTypes()){
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
    ){
        if(request.getRenewalDetail() == null){
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

        if(request.getRenewalDetail().getPremiumChangeReasonTypes() != null){
            for(String reasonType : request.getRenewalDetail().getPremiumChangeReasonTypes()){
                consultationRenewalPremiumChangeReasonRepository.save(
                        ConsultationRenewalPremiumChangeReason.builder()
                                .consultationRenewalDetail(detail)
                                .reasonType(reasonType)
                                .otherReason(request.getRenewalDetail().getOtherReason())
                                .createdAt(now)
                                .createdBy(fp.getId())
                                .updatedAt(now)
                                .updatedBy(fp.getId())
                                .build()
                );
            }
        }

        if(request.getRenewalDetail().getInterestTypes() != null){
            for(String interestType : request.getRenewalDetail().getInterestTypes()){

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
    ){
        if(request.getCancelDetail() == null){
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
}
