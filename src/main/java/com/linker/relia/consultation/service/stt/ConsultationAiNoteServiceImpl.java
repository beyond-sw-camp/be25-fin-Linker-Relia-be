package com.linker.relia.consultation.service.stt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linker.relia.common.audit.AuditContextHolder;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.consultation.domain.ConsultationNewCoverageType;
import com.linker.relia.consultation.domain.ConsultationType;
import com.linker.relia.consultation.domain.stt.ConsultationAiNote;
import com.linker.relia.consultation.domain.stt.ConsultationAiNoteStatus;
import com.linker.relia.consultation.domain.stt.ConsultationSttSession;
import com.linker.relia.consultation.dto.response.ConsultationAiDraftResponse;
import com.linker.relia.consultation.dto.response.ConsultationAiGenerationResult;
import com.linker.relia.consultation.dto.response.ConsultationAiNoteApplyResponse;
import com.linker.relia.consultation.dto.response.ConsultationAiStructuredDraft;
import com.linker.relia.consultation.exception.ConsultationErrorCode;
import com.linker.relia.consultation.repository.stt.ConsultationAiNoteRepository;
import com.linker.relia.insurance.domain.InsuranceProduct;
import com.linker.relia.insurance.repository.InsuranceProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationAiNoteServiceImpl implements ConsultationAiNoteService {
    private static final Pattern PRODUCT_CODE_PATTERN =
            Pattern.compile("\\bLP\\s*0*(\\d{1,3})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRAILING_NUMBER_PATTERN = Pattern.compile("(\\d{1,3})\\s*$");

    private final ConsultationSttSessionService consultationSttSessionService;
    private final ConsultationAiNoteRepository consultationAiNoteRepository;
    private final ConsultationAiDraftGenerator consultationAiDraftGenerator;
    private final InsuranceProductRepository insuranceProductRepository;
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
                NormalizationResult normalized = normalizeWithWarnings(structuredDraft);
                if (!normalized.warnings().isEmpty()) {
                    log.info("Normalized AI structured draft during STT completion. sessionId={} warnings={}",
                            sessionId, normalized.warnings());
                }
                aiNote.completeGpt(
                        result.getSummaryText(),
                        objectMapper.writeValueAsString(normalized.draft())
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

            aiNote.markApplied();
            NormalizationResult normalized = normalizeStoredStructuredData(aiNote.getGptStructuredData());

            if (normalized.draft() != null) {
                aiNote.completeGpt(
                        aiNote.getGptSummaryText(),
                        writeStructuredData(normalized.draft())
                );
                aiNote.markApplied();
            }

            if (!normalized.warnings().isEmpty()) {
                log.info("Applied AI draft with warnings. aiNoteId={} warnings={}", aiNoteId, normalized.warnings());
            }
            consultationAiNoteRepository.flush();

            return ConsultationAiNoteApplyResponse.builder()
                    .aiNoteId(aiNote.getId())
                    .status(aiNote.getDraftStatus())
                    .appliedAt(aiNote.getUpdatedAt())
                    .structuredData(normalized.draft())
                    .warnings(normalized.warnings())
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
        return normalizeStoredStructuredData(rawJson).draft();
    }

    private NormalizationResult normalizeStoredStructuredData(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return new NormalizationResult(null, List.of());
        }

        try {
            return normalizeWithWarnings(objectMapper.readValue(rawJson, ConsultationAiStructuredDraft.class));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_INVALID_DATA);
        }
    }

    private String writeStructuredData(ConsultationAiStructuredDraft draft) {
        try {
            return objectMapper.writeValueAsString(draft);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_INVALID_DATA);
        }
    }

    private NormalizationResult normalizeWithWarnings(ConsultationAiStructuredDraft draft) {
        if (draft == null) {
            return new NormalizationResult(null, List.of());
        }

        List<String> warnings = new ArrayList<>();

        if (draft.getConsultationType() == ConsultationType.NEW_CONTRACT && draft.getCustomerId() != null) {
            if (draft.getCustomerInfo() != null) {
                warnings.add("customerId and customerInfo existed together. customerInfo was dropped.");
                log.info("Normalized invalid structured draft value. field=customerInfo reason=customerId_and_customerInfo_conflict");
            }
            draft.setCustomerInfo(null);
        }

        if (draft.getNewDetail() != null) {
            normalizeNewDetail(draft.getNewDetail(), warnings);
        }

        return new NormalizationResult(draft, List.copyOf(warnings));
    }

    private void normalizeNewDetail(ConsultationAiStructuredDraft.NewDetail newDetail, List<String> warnings) {
        Set<String> normalizedCoverageTypes = new LinkedHashSet<>();
        Set<String> normalizedProductCodes = new LinkedHashSet<>();

        if (newDetail.getCoverageTypes() != null) {
            for (String rawValue : newDetail.getCoverageTypes()) {
                if (rawValue == null || rawValue.isBlank()) {
                    continue;
                }

                String normalizedCoverage = normalizeCoverageType(rawValue);
                if (normalizedCoverage != null) {
                    normalizedCoverageTypes.add(normalizedCoverage);
                    continue;
                }

                String candidateProductCode = resolveProductCode(rawValue);
                if (candidateProductCode != null) {
                    normalizedProductCodes.add(candidateProductCode);
                    warnings.add("coverageTypes contained product-like value '" + rawValue
                            + "'. moved to proposedProductCodes as " + candidateProductCode + ".");
                    log.info("Normalized invalid coverageTypes value to proposedProductCodes. field=coverageTypes rawValue={} normalizedCode={}",
                            rawValue, candidateProductCode);
                    continue;
                }

                warnings.add("coverageTypes contained unsupported value '" + rawValue + "'. dropped.");
                log.warn("Dropped invalid structured draft value. field=coverageTypes rawValue={}", rawValue);
            }
        }

        if (newDetail.getProposedProductCodes() != null) {
            for (String rawValue : newDetail.getProposedProductCodes()) {
                if (rawValue == null || rawValue.isBlank()) {
                    continue;
                }

                String candidateProductCode = resolveProductCode(rawValue);
                if (candidateProductCode != null) {
                    normalizedProductCodes.add(candidateProductCode);
                    continue;
                }

                warnings.add("proposedProductCodes contained non-code value '" + rawValue + "'. dropped.");
                log.warn("Dropped invalid structured draft value. field=proposedProductCodes rawValue={}", rawValue);
            }
        }

        newDetail.setCoverageTypes(toNullableList(normalizedCoverageTypes));
        newDetail.setProposedProductCodes(toNullableList(normalizedProductCodes));
    }

    private String normalizeCoverageType(String rawValue) {
        String compact = rawValue.replace(" ", "").trim().toUpperCase(Locale.ROOT);

        if (isCoverageEnumCode(compact)) {
            return compact;
        }
        if (containsAny(compact, "암", "암보장", "암진단비")) {
            return ConsultationNewCoverageType.CANCER.name();
        }
        if (containsAny(compact, "심장", "심장보장", "심혈관")) {
            return ConsultationNewCoverageType.HEART.name();
        }
        if (containsAny(compact, "생명", "생명보장", "종신")) {
            return ConsultationNewCoverageType.LIFE.name();
        }
        if (containsAny(compact, "사망", "사망보장")) {
            return ConsultationNewCoverageType.DEATH.name();
        }
        if (containsAny(compact, "장기요양", "장기요양보장", "장기요양보험", "장기요양특약", "간병")) {
            return ConsultationNewCoverageType.LONG_TERM_CARE.name();
        }

        return null;
    }

    private boolean isCoverageEnumCode(String value) {
        for (ConsultationNewCoverageType type : ConsultationNewCoverageType.values()) {
            if (type.name().equals(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String resolveProductCode(String rawValue) {
        String trimmed = rawValue.trim();
        Matcher codeMatcher = PRODUCT_CODE_PATTERN.matcher(trimmed);
        if (codeMatcher.find()) {
            return formatProductCode(codeMatcher.group(1));
        }

        Optional<InsuranceProduct> exactMatch = insuranceProductRepository
                .findByInsuranceProductNameAndDeletedAtIsNull(trimmed);
        if (exactMatch.isPresent()) {
            return exactMatch.get().getInsuranceProductCode();
        }

        if (looksLikeProductLabel(trimmed)) {
            Matcher trailingNumberMatcher = TRAILING_NUMBER_PATTERN.matcher(trimmed);
            if (trailingNumberMatcher.find()) {
                return formatProductCode(trailingNumberMatcher.group(1));
            }
        }

        return null;
    }

    private boolean looksLikeProductLabel(String value) {
        String compact = value.replace(" ", "");
        return compact.contains("플랜")
                || compact.contains("보험")
                || compact.contains("특약")
                || compact.contains("보장");
    }

    private String formatProductCode(String rawNumber) {
        int number = Integer.parseInt(rawNumber);
        if (number <= 0 || number > 999) {
            return null;
        }
        return "LP" + String.format("%03d", number);
    }

    private List<String> toNullableList(Set<String> values) {
        if (values.isEmpty()) {
            return null;
        }
        return new ArrayList<>(values);
    }

    private record NormalizationResult(
            ConsultationAiStructuredDraft draft,
            List<String> warnings
    ) {
    }
}
