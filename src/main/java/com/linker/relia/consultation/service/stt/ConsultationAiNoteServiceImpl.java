package com.linker.relia.consultation.service.stt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linker.relia.common.audit.AuditContextHolder;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.consultation.domain.ConsultationChannel;
import com.linker.relia.consultation.domain.ConsultationNewCoverageType;
import com.linker.relia.consultation.domain.ConsultationType;
import com.linker.relia.consultation.domain.stt.ConsultationAiNote;
import com.linker.relia.consultation.domain.stt.ConsultationAiNoteStatus;
import com.linker.relia.consultation.domain.stt.ConsultationSttSession;
import com.linker.relia.consultation.dto.request.ConsultationCancelDetailRequest;
import com.linker.relia.consultation.dto.request.ConsultationClaimDetailRequest;
import com.linker.relia.consultation.dto.request.ConsultationCreateRequest;
import com.linker.relia.consultation.dto.request.ConsultationNewDetailRequest;
import com.linker.relia.consultation.dto.request.ConsultationRenewalDetailRequest;
import com.linker.relia.consultation.dto.request.CustomerInfoRequest;
import com.linker.relia.consultation.dto.response.ConsultationAiDraftResponse;
import com.linker.relia.consultation.dto.response.ConsultationAiGenerationResult;
import com.linker.relia.consultation.dto.response.ConsultationAiNoteApplyResponse;
import com.linker.relia.consultation.dto.response.ConsultationAiStructuredDraft;
import com.linker.relia.consultation.dto.response.ConsultationCreateResponse;
import com.linker.relia.consultation.exception.ConsultationErrorCode;
import com.linker.relia.consultation.repository.stt.ConsultationAiNoteRepository;
import com.linker.relia.consultation.service.ConsultationService;
import com.linker.relia.customer.domain.CustomerMaritalStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationAiNoteServiceImpl implements ConsultationAiNoteService {
    private final ConsultationSttSessionService consultationSttSessionService;
    private final ConsultationAiNoteRepository consultationAiNoteRepository;
    private final ConsultationAiDraftGenerator consultationAiDraftGenerator;
    private final ConsultationService consultationService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void processSttCompleted(UUID sessionId, UUID fpId, String sttRawText) {
        AuditContextHolder.setCurrentAuditor(fpId);
        try {
            ConsultationSttSession session = consultationSttSessionService.getOwnedSession(sessionId, fpId);
            ConsultationAiNote aiNote = findOrCreateAiNote(session);
            aiNote.completeStt(sttRawText);

            try {
                ConsultationAiGenerationResult result = consultationAiDraftGenerator.generate(session, sttRawText);
                ConsultationAiStructuredDraft structuredDraft =
                        enrichStructuredDraft(session, result.getStructuredData());
                aiNote.completeGpt(
                        result.getSummaryText(),
                        objectMapper.writeValueAsString(structuredDraft)
                );
            } catch (Exception e) {
                log.warn("AI consultation draft generation failed. sessionId={}", sessionId, e);
                aiNote.markFailed(e.getMessage());
            }
        } finally {
            AuditContextHolder.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationAiDraftResponse getAiDraft(UUID sessionId, UUID fpId) {
        consultationSttSessionService.getOwnedSession(sessionId, fpId);
        ConsultationAiNote aiNote = consultationAiNoteRepository
                .findTopBySttSession_IdAndDeletedAtIsNullOrderByCreatedAtDesc(sessionId)
                .orElseThrow(() -> new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_NOT_FOUND));

        return ConsultationAiDraftResponse.builder()
                .aiNoteId(aiNote.getId())
                .sessionId(sessionId)
                .consultationType(aiNote.getConsultationType())
                .draftStatus(aiNote.getDraftStatus())
                .sttRawText(aiNote.getSttRawText())
                .summaryText(aiNote.getGptSummaryText())
                .structuredData(parseStructuredData(aiNote.getGptStructuredData()))
                .errorMessage(aiNote.getErrorMessage())
                .build();
    }

    @Override
    @Transactional
    public ConsultationAiNoteApplyResponse applyAiDraft(UUID aiNoteId, UUID fpId) {
        AuditContextHolder.setCurrentAuditor(fpId);
        try {
            ConsultationAiNote aiNote = consultationAiNoteRepository
                    .findByIdAndDeletedAtIsNull(aiNoteId)
                    .orElseThrow(() -> new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_NOT_FOUND));

            ConsultationSttSession session = aiNote.getSttSession();
            if (session == null) {
                throw new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_INVALID_DATA);
            }

            consultationSttSessionService.getOwnedSession(session.getId(), fpId);
            validateApplicableStatus(aiNote.getDraftStatus());

            ConsultationAiStructuredDraft structuredDraft = parseStructuredData(aiNote.getGptStructuredData());
            if (structuredDraft == null) {
                throw new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_INVALID_DATA);
            }

            ConsultationCreateRequest createRequest = toCreateRequest(structuredDraft);
            ConsultationCreateResponse created = consultationService.createConsultation(createRequest, session.getFp());
            aiNote.markApplied();
            consultationAiNoteRepository.flush();

            return ConsultationAiNoteApplyResponse.builder()
                    .aiNoteId(aiNote.getId())
                    .consultationId(created.getConsultationId())
                    .draftStatus(aiNote.getDraftStatus())
                    .appliedAt(aiNote.getUpdatedAt())
                    .build();
        } finally {
            AuditContextHolder.clear();
        }
    }

    private ConsultationAiNote findOrCreateAiNote(ConsultationSttSession session) {
        return consultationAiNoteRepository
                .findTopBySttSession_IdAndDeletedAtIsNullOrderByCreatedAtDesc(session.getId())
                .orElseGet(() -> consultationAiNoteRepository.save(
                        ConsultationAiNote.builder()
                                .audioRecord(null)
                                .sttSession(session)
                                .consultationType(session.getConsultationType())
                                .draftStatus(ConsultationAiNoteStatus.PENDING)
                                .build()
                ));
    }

    private void validateApplicableStatus(ConsultationAiNoteStatus status) {
        if (status == ConsultationAiNoteStatus.APPLIED) {
            throw new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_ALREADY_APPLIED);
        }
        if (status != ConsultationAiNoteStatus.GPT_COMPLETED) {
            throw new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_NOT_APPLICABLE);
        }
    }

    private ConsultationCreateRequest toCreateRequest(ConsultationAiStructuredDraft draft) {
        ConsultationCreateRequest request = new ConsultationCreateRequest();

        setField(request, "customerId", draft.getCustomerId());
        setField(request, "customerInfo", toCustomerInfoRequest(draft.getCustomerInfo()));
        setField(request, "contractId", draft.getContractId());
        setField(request, "consultationType", draft.getConsultationType());
        setField(request, "consultationChannel", parseEnum(
                ConsultationChannel.class,
                draft.getConsultationChannel()
        ));
        setField(request, "consultedAt", draft.getConsultedAt());
        setField(request, "specialNote", draft.getSpecialNote());
        setField(request, "nextScheduledAt", draft.getNextScheduledAt());
        setField(request, "newDetail", toNewDetailRequest(draft.getNewDetail()));
        setField(request, "claimDetail", toClaimDetailRequest(draft.getClaimDetail()));
        setField(request, "renewalDetail", toRenewalDetailRequest(draft.getRenewalDetail()));
        setField(request, "cancelDetail", toCancelDetailRequest(draft.getCancelDetail()));

        return request;
    }

    private CustomerInfoRequest toCustomerInfoRequest(ConsultationAiStructuredDraft.CustomerInfo customerInfo) {
        if (customerInfo == null) {
            return null;
        }

        CustomerInfoRequest request = new CustomerInfoRequest();
        setField(request, "customerName", customerInfo.getCustomerName());
        setField(request, "customerGender", customerInfo.getCustomerGender());
        setField(request, "customerBirthDate", customerInfo.getCustomerBirthDate());
        setField(request, "customerPhone", customerInfo.getCustomerPhone());
        setField(request, "customerEmail", customerInfo.getCustomerEmail());
        setField(request, "customerZipcode", customerInfo.getCustomerZipcode());
        setField(request, "customerAddressRoad", customerInfo.getCustomerAddressRoad());
        setField(request, "customerAddressDetail", customerInfo.getCustomerAddressDetail());
        setField(request, "customerJob", customerInfo.getCustomerJob());
        setField(request, "customerCompanyName", customerInfo.getCustomerCompanyName());
        setField(request, "customerAnnualIncome", toBigDecimal(customerInfo.getCustomerAnnualIncome()));
        setField(request, "customerAssetSize", toBigDecimal(customerInfo.getCustomerAssetSize()));
        setField(request, "customerDebtStatus", customerInfo.getCustomerDebtStatus());
        setField(request, "customerIsSmoker", customerInfo.getCustomerIsSmoker());
        setField(request, "customerIsDrinker", customerInfo.getCustomerIsDrinker());
        setField(request, "customerMaritalStatus", parseEnum(
                CustomerMaritalStatus.class,
                customerInfo.getCustomerMaritalStatus()
        ));
        setField(request, "customerDependentsCount", customerInfo.getCustomerDependentsCount());
        setField(request, "underlyingDiseaseCodes", customerInfo.getUnderlyingDiseaseCodes());
        return request;
    }

    private ConsultationNewDetailRequest toNewDetailRequest(ConsultationAiStructuredDraft.NewDetail newDetail) {
        if (newDetail == null) {
            return null;
        }

        ConsultationNewDetailRequest request = new ConsultationNewDetailRequest();
        setField(request, "monthlyIncome", toBigDecimal(newDetail.getMonthlyIncome()));
        setField(request, "hasExistingInsurance", newDetail.getHasExistingInsurance());
        setField(request, "monthlyInsurancePremium", toBigDecimal(newDetail.getMonthlyInsurancePremium()));
        setField(request, "existingInsuranceNote", newDetail.getExistingInsuranceNote());
        setField(request, "insurancePriority", newDetail.getInsurancePriority());
        setField(request, "coverageTypes", mapCoverageTypes(newDetail.getCoverageTypes()));
        setField(request, "proposedProductCodes", newDetail.getProposedProductCodes());
        return request;
    }

    private ConsultationClaimDetailRequest toClaimDetailRequest(ConsultationAiStructuredDraft.ClaimDetail claimDetail) {
        if (claimDetail == null) {
            return null;
        }

        ConsultationClaimDetailRequest request = new ConsultationClaimDetailRequest();
        setField(request, "claimType", claimDetail.getClaimType());
        setField(request, "claimReason", claimDetail.getClaimReason());
        setField(request, "incidentDate", claimDetail.getIncidentDate());
        setField(request, "reviewItems", claimDetail.getReviewItems());
        setField(request, "result", claimDetail.getResult());
        setField(request, "nextActions", claimDetail.getNextActions());
        return request;
    }

    private ConsultationRenewalDetailRequest toRenewalDetailRequest(
            ConsultationAiStructuredDraft.RenewalDetail renewalDetail
    ) {
        if (renewalDetail == null) {
            return null;
        }

        ConsultationRenewalDetailRequest request = new ConsultationRenewalDetailRequest();
        setField(request, "renewalReason", renewalDetail.getRenewalReason());
        setField(request, "renewalScheduledDate", renewalDetail.getRenewalScheduledDate());
        setField(request, "currentPremium", toBigDecimal(renewalDetail.getCurrentPremium()));
        setField(request, "renewalPremium", toBigDecimal(renewalDetail.getRenewalPremium()));
        setField(request, "premiumChangeRate", renewalDetail.getPremiumChangeRate());
        setField(request, "coverageChangeType", renewalDetail.getCoverageChangeType());
        setField(request, "coverageChangeDetail", renewalDetail.getCoverageChangeDetail());
        setField(request, "customerReaction", renewalDetail.getCustomerReaction());
        setField(request, "consultationResult", renewalDetail.getConsultationResult());
        setField(request, "premiumChangeReasonTypes", renewalDetail.getPremiumChangeReasons());
        setField(request, "otherReason", renewalDetail.getOtherReason());
        setField(request, "interestTypes", renewalDetail.getInterestTypes());
        setField(request, "nextActions", joinActions(renewalDetail.getNextActions()));
        setField(request, "decisionExpectedDate", renewalDetail.getDecisionExpectedDate());
        return request;
    }

    private ConsultationCancelDetailRequest toCancelDetailRequest(
            ConsultationAiStructuredDraft.CancelDetail cancelDetail
    ) {
        if (cancelDetail == null) {
            return null;
        }

        ConsultationCancelDetailRequest request = new ConsultationCancelDetailRequest();
        setField(request, "premiumBurden", cancelDetail.getPremiumBurden());
        setField(request, "renewalPremiumBurden", cancelDetail.getRenewalPremiumBurden());
        setField(request, "paymentDifficulty", cancelDetail.getPaymentDifficulty());
        setField(request, "coverageDissatisfaction", cancelDetail.getCoverageDissatisfaction());
        setField(request, "duplicateInsurance", cancelDetail.getDuplicateInsurance());
        setField(request, "productRemodelingReview", cancelDetail.getProductRemodelingReview());
        setField(request, "comparingOtherCompany", cancelDetail.getComparingOtherCompany());
        setField(request, "movingToOtherCompany", cancelDetail.getMovingToOtherCompany());
        setField(request, "plannerContactDissatisfaction", cancelDetail.getPlannerContactDissatisfaction());
        setField(request, "managementDissatisfaction", cancelDetail.getManagementDissatisfaction());
        setField(request, "retentionPossibility", cancelDetail.getRetentionPossibility());
        return request;
    }

    private List<ConsultationNewCoverageType> mapCoverageTypes(List<String> coverageTypes) {
        if (coverageTypes == null) {
            return null;
        }

        return coverageTypes.stream()
                .map(value -> parseEnum(ConsultationNewCoverageType.class, value))
                .toList();
    }

    private String joinActions(List<String> actions) {
        if (actions == null || actions.isEmpty()) {
            return null;
        }
        return String.join(", ", actions);
    }

    private BigDecimal toBigDecimal(Long value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Enum.valueOf(enumType, value.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_INVALID_DATA);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to map AI draft field: " + fieldName, e);
        }
    }

    private ConsultationAiStructuredDraft enrichStructuredDraft(
            ConsultationSttSession session,
            ConsultationAiStructuredDraft structuredDraft
    ) {
        ConsultationAiStructuredDraft draft =
                structuredDraft != null ? structuredDraft : new ConsultationAiStructuredDraft();

        draft.setConsultationType(session.getConsultationType());
        if (draft.getConsultedAt() == null) {
            draft.setConsultedAt(session.getStartedAt());
        }
        if (draft.getCustomerId() == null && session.getCustomer() != null) {
            draft.setCustomerId(session.getCustomer().getId());
        }
        if (session.getConsultationType() != ConsultationType.NEW_CONTRACT) {
            draft.setCustomerInfo(null);
        }

        return draft;
    }

    private ConsultationAiStructuredDraft parseStructuredData(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(rawJson, ConsultationAiStructuredDraft.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_INVALID_DATA);
        }
    }
}
