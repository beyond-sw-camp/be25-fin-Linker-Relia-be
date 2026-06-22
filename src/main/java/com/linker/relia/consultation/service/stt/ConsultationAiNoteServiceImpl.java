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
import com.linker.relia.consultation.dto.response.ConsultationAiResolutionResponse;
import com.linker.relia.consultation.dto.response.ConsultationAiStructuredDraft;
import com.linker.relia.consultation.exception.ConsultationErrorCode;
import com.linker.relia.consultation.repository.stt.ConsultationAiNoteRepository;
import com.linker.relia.contract.repository.ContractRepository;
import com.linker.relia.customer.domain.DiseaseCode;
import com.linker.relia.customer.dto.CustomerOwnedContractResponse;
import com.linker.relia.customer.repository.DiseaseCodeRepository;
import com.linker.relia.insurance.domain.InsuranceProduct;
import com.linker.relia.insurance.repository.InsuranceProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
    private static final Pattern PRODUCT_KEYWORD_SPLIT_PATTERN = Pattern.compile("[\\s,/()\\[\\]{}]+");
    private static final Pattern PRODUCT_HINT_PATTERN = Pattern.compile(
            "([가-힣A-Za-z0-9]+(?:\\s+[가-힣A-Za-z0-9]+){0,5}\\s*(?:보험|보장|특약)(?:\\s*본)?(?:\\s*\\d{1,3})?)"
    );
    private static final Set<String> PRODUCT_SEARCH_STOPWORDS = Set.of(
            "보험", "보장", "특약", "상품", "플랜", "추천", "가입", "설계", "상담", "고객", "본"
    );
    private static final List<String> PRODUCT_HINT_PREFIXES = List.of(
            "관심있는 상품은",
            "관심 상품은",
            "추천 상품은",
            "추천받은 상품은",
            "상품은",
            "상품명은",
            "보험은"
    );

    private final ConsultationSttSessionService consultationSttSessionService;
    private final ConsultationAiNoteRepository consultationAiNoteRepository;
    private final ConsultationAiDraftGenerator consultationAiDraftGenerator;
    private final InsuranceProductRepository insuranceProductRepository;
    private final ContractRepository contractRepository;
    private final DiseaseCodeRepository diseaseCodeRepository;
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
                NormalizationResult normalized = normalizeWithWarnings(structuredDraft, sttRawText);
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

        ConsultationSttSession session = aiNote.getSttSession();
        ConsultationAiStructuredDraft draft = normalizeStoredStructuredData(
                aiNote.getGptStructuredData(),
                aiNote.getSttRawText()
        ).draft();
        DraftResolution resolved = resolveMappings(session, draft);

        return ConsultationAiDraftResponse.builder()
                .aiNoteId(aiNote.getId())
                .sessionId(sessionId)
                .consultationType(aiNote.getConsultationType())
                .draftStatus(aiNote.getDraftStatus())
                .sttRawText(aiNote.getSttRawText())
                .summaryText(aiNote.getGptSummaryText())
                .structuredData(resolved.draft())
                .resolutions(resolved.resolution())
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
            NormalizationResult normalized = normalizeStoredStructuredData(aiNote.getGptStructuredData(), aiNote.getSttRawText());
            DraftResolution resolved = resolveMappings(session, normalized.draft());

            if (resolved.draft() != null) {
                aiNote.completeGpt(
                        aiNote.getGptSummaryText(),
                        writeStructuredData(resolved.draft())
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
                    .structuredData(resolved.draft())
                    .resolutions(resolved.resolution())
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
        return normalizeStoredStructuredData(rawJson, null).draft();
    }

    private NormalizationResult normalizeStoredStructuredData(String rawJson, String referenceText) {
        if (rawJson == null || rawJson.isBlank()) {
            return new NormalizationResult(null, List.of());
        }

        try {
            return normalizeWithWarnings(objectMapper.readValue(rawJson, ConsultationAiStructuredDraft.class), referenceText);
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

    private NormalizationResult normalizeWithWarnings(ConsultationAiStructuredDraft draft, String referenceText) {
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
            normalizeNewDetail(draft.getNewDetail(), warnings, referenceText);
        }

        backfillAiHintsFromReferenceText(draft, referenceText);
        normalizeAiHints(draft);
        return new NormalizationResult(draft, List.copyOf(warnings));
    }

    private void normalizeNewDetail(
            ConsultationAiStructuredDraft.NewDetail newDetail,
            List<String> warnings,
            String referenceText
    ) {
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
        normalizeInsurancePriority(newDetail, referenceText, warnings);
    }

    private void normalizeInsurancePriority(
            ConsultationAiStructuredDraft.NewDetail newDetail,
            String referenceText,
            List<String> warnings
    ) {
        if (newDetail.getInsurancePriority() != null && !newDetail.getInsurancePriority().isBlank()) {
            String normalized = mapInsurancePriority(newDetail.getInsurancePriority());
            if (!Objects.equals(normalized, newDetail.getInsurancePriority())) {
                warnings.add("insurancePriority was normalized to '" + normalized + "'.");
            }
            newDetail.setInsurancePriority(normalized);
            return;
        }

        String inferred = inferInsurancePriority(referenceText);
        if (inferred != null) {
            newDetail.setInsurancePriority(inferred);
            warnings.add("insurancePriority was inferred from transcript as '" + inferred + "'.");
        }
    }

    private String mapInsurancePriority(String rawValue) {
        String compact = normalizeText(rawValue);
        if (compact == null || compact.isBlank()) {
            return rawValue;
        }
        if (containsAny(compact, "보험료", "저렴", "싼", "낮은", "가성비", "납입부담")) {
            return "보험료";
        }
        if (containsAny(compact, "보장", "보장범위", "보장넓", "보장크게")) {
            return "보장 범위";
        }
        if (containsAny(compact, "보험사", "회사", "브랜드")) {
            return "보험사";
        }
        if (containsAny(compact, "갱신", "비갱신", "안정성")) {
            return "갱신 안정성";
        }
        return rawValue.trim();
    }

    private String inferInsurancePriority(String referenceText) {
        String compact = normalizeText(referenceText);
        if (compact == null || compact.isBlank()) {
            return null;
        }
        if (containsAny(compact, "보험료", "저렴", "싼", "낮은", "가성비", "납입부담")) {
            return "보험료";
        }
        if (containsAny(compact, "보장", "보장범위", "보장넓", "보장크게")) {
            return "보장 범위";
        }
        if (containsAny(compact, "보험사", "회사", "브랜드")) {
            return "보험사";
        }
        if (containsAny(compact, "갱신", "비갱신", "안정성")) {
            return "갱신 안정성";
        }
        return null;
    }

    private DraftResolution resolveMappings(ConsultationSttSession session, ConsultationAiStructuredDraft draft) {
        if (draft == null) {
            return new DraftResolution(null, ConsultationAiResolutionResponse.empty());
        }

        List<ConsultationAiResolutionResponse.FieldResolution> fields = new ArrayList<>();

        if (draft.getAiHints() != null) {
            resolveContractHint(session, draft, fields);
            resolveProductHints(draft, fields);
            resolveDiseaseHints(draft, fields);
            normalizeAiHints(draft);
        }

        return new DraftResolution(
                draft,
                ConsultationAiResolutionResponse.builder()
                        .hasPendingResolution(fields.stream().anyMatch(field -> "NEEDS_USER_CONFIRMATION".equals(field.getStatus())))
                        .fields(List.copyOf(fields))
                        .build()
        );
    }

    private void resolveContractHint(
            ConsultationSttSession session,
            ConsultationAiStructuredDraft draft,
            List<ConsultationAiResolutionResponse.FieldResolution> fields
    ) {
        ConsultationAiStructuredDraft.AiHints aiHints = draft.getAiHints();
        if (aiHints == null || aiHints.getTargetContractHint() == null || aiHints.getTargetContractHint().isBlank()) {
            return;
        }
        if (draft.getContractId() != null || session == null || session.getCustomer() == null) {
            return;
        }

        String rawHint = aiHints.getTargetContractHint().trim();
        List<CustomerOwnedContractResponse> contracts = contractRepository.findOwnCustomerContracts(session.getCustomer().getId());
        List<CustomerOwnedContractResponse> candidates = contracts.stream()
                .filter(contract -> matchesContractHint(contract, rawHint))
                .toList();

        if (candidates.size() == 1) {
            CustomerOwnedContractResponse candidate = candidates.get(0);
            draft.setContractId(candidate.getContractId());
            aiHints.setTargetContractHint(null);
            fields.add(ConsultationAiResolutionResponse.FieldResolution.builder()
                    .fieldPath("contractId")
                    .rawValue(rawHint)
                    .status("AUTO_MAPPED")
                    .message("고객 보유 계약 목록에서 1건으로 자동 매핑되었습니다.")
                    .candidates(List.of(toContractCandidate(candidate)))
                    .build());
            return;
        }

        fields.add(ConsultationAiResolutionResponse.FieldResolution.builder()
                .fieldPath("contractId")
                .rawValue(rawHint)
                .status(candidates.isEmpty() ? "NO_CANDIDATE" : "NEEDS_USER_CONFIRMATION")
                .message(candidates.isEmpty()
                        ? "일치하는 고객 보유 계약을 찾지 못했습니다."
                        : "복수 계약이 매칭되어 사용자 확인이 필요합니다.")
                .candidates(candidates.stream().map(this::toContractCandidate).toList())
                .build());
    }

    private void resolveProductHints(
            ConsultationAiStructuredDraft draft,
            List<ConsultationAiResolutionResponse.FieldResolution> fields
    ) {
        if (draft.getAiHints() == null || draft.getAiHints().getMentionedProductNames() == null || draft.getNewDetail() == null) {
            return;
        }

        Set<String> productCodes = new LinkedHashSet<>();
        if (draft.getNewDetail().getProposedProductCodes() != null) {
            productCodes.addAll(draft.getNewDetail().getProposedProductCodes());
        }

        List<String> unresolvedNames = new ArrayList<>();
        for (String rawName : draft.getAiHints().getMentionedProductNames()) {
            if (rawName == null || rawName.isBlank()) {
                continue;
            }

            ProductMatchResult result = findProductCandidates(rawName.trim());
            if (result.autoMappedCode() != null) {
                productCodes.add(result.autoMappedCode());
                fields.add(ConsultationAiResolutionResponse.FieldResolution.builder()
                        .fieldPath("newDetail.proposedProductCodes")
                        .rawValue(rawName.trim())
                        .status("AUTO_MAPPED")
                        .message("상품명을 기준으로 1건으로 자동 매핑되었습니다.")
                        .candidates(result.candidates())
                        .build());
                continue;
            }

            unresolvedNames.add(rawName.trim());
            fields.add(ConsultationAiResolutionResponse.FieldResolution.builder()
                    .fieldPath("newDetail.proposedProductCodes")
                    .rawValue(rawName.trim())
                    .status(result.candidates().isEmpty() ? "NO_CANDIDATE" : "NEEDS_USER_CONFIRMATION")
                    .message(result.candidates().isEmpty()
                            ? "일치하는 상품 후보를 찾지 못했습니다."
                            : "복수 상품 후보가 있어 사용자 확인이 필요합니다.")
                    .candidates(result.candidates())
                    .build());
        }

        draft.getNewDetail().setProposedProductCodes(productCodes.isEmpty() ? null : new ArrayList<>(productCodes));
        draft.getAiHints().setMentionedProductNames(unresolvedNames.isEmpty() ? null : unresolvedNames);
    }

    private void resolveDiseaseHints(
            ConsultationAiStructuredDraft draft,
            List<ConsultationAiResolutionResponse.FieldResolution> fields
    ) {
        if (draft.getAiHints() == null || draft.getAiHints().getMentionedDiseaseNames() == null || draft.getCustomerInfo() == null) {
            return;
        }

        Set<String> diseaseCodes = new LinkedHashSet<>();
        if (draft.getCustomerInfo().getUnderlyingDiseaseCodes() != null) {
            diseaseCodes.addAll(draft.getCustomerInfo().getUnderlyingDiseaseCodes());
        }

        List<String> unresolvedNames = new ArrayList<>();
        for (String rawName : draft.getAiHints().getMentionedDiseaseNames()) {
            if (rawName == null || rawName.isBlank()) {
                continue;
            }

            DiseaseMatchResult result = findDiseaseCandidates(rawName.trim());
            if (result.autoMappedCode() != null) {
                diseaseCodes.add(result.autoMappedCode());
                fields.add(ConsultationAiResolutionResponse.FieldResolution.builder()
                        .fieldPath("customerInfo.underlyingDiseaseCodes")
                        .rawValue(rawName.trim())
                        .status("AUTO_MAPPED")
                        .message("질병명을 기준으로 1건으로 자동 매핑되었습니다.")
                        .candidates(result.candidates())
                        .build());
                continue;
            }

            unresolvedNames.add(rawName.trim());
            fields.add(ConsultationAiResolutionResponse.FieldResolution.builder()
                    .fieldPath("customerInfo.underlyingDiseaseCodes")
                    .rawValue(rawName.trim())
                    .status(result.candidates().isEmpty() ? "NO_CANDIDATE" : "NEEDS_USER_CONFIRMATION")
                    .message(result.candidates().isEmpty()
                            ? "일치하는 질병 코드 후보를 찾지 못했습니다."
                            : "복수 질병 후보가 있어 사용자 확인이 필요합니다.")
                    .candidates(result.candidates())
                    .build());
        }

        draft.getCustomerInfo().setUnderlyingDiseaseCodes(diseaseCodes.isEmpty() ? null : new ArrayList<>(diseaseCodes));
        draft.getAiHints().setMentionedDiseaseNames(unresolvedNames.isEmpty() ? null : unresolvedNames);
    }

    private ProductMatchResult findProductCandidates(String rawName) {
        String resolvedProductCode = resolveProductCode(rawName);
        if (resolvedProductCode != null) {
            Optional<InsuranceProduct> codeMatch =
                    insuranceProductRepository.findByInsuranceProductCodeAndDeletedAtIsNull(resolvedProductCode);
            if (codeMatch.isPresent()) {
                InsuranceProduct product = codeMatch.get();
                return new ProductMatchResult(
                        product.getInsuranceProductCode(),
                        List.of(toProductCandidate(product))
                );
            }
        }

        Optional<InsuranceProduct> exactMatch = insuranceProductRepository.findByInsuranceProductNameAndDeletedAtIsNull(rawName);
        if (exactMatch.isPresent()) {
            InsuranceProduct product = exactMatch.get();
            return new ProductMatchResult(
                    product.getInsuranceProductCode(),
                    List.of(toProductCandidate(product))
            );
        }

        List<InsuranceProduct> searchedProducts = searchProductCandidates(rawName);
        List<ScoredProductMatch> rankedMatches = searchedProducts.stream()
                .map(product -> new ScoredProductMatch(product, scoreProductMatch(product, rawName)))
                .filter(match -> match.score() > 0)
                .sorted((left, right) -> Integer.compare(right.score(), left.score()))
                .toList();

        if (rankedMatches.size() == 1) {
            InsuranceProduct product = rankedMatches.get(0).product();
            return new ProductMatchResult(
                    product.getInsuranceProductCode(),
                    List.of(toProductCandidate(product))
            );
        }

        if (shouldAutoMap(rankedMatches)) {
            InsuranceProduct product = rankedMatches.get(0).product();
            return new ProductMatchResult(
                    product.getInsuranceProductCode(),
                    List.of(toProductCandidate(product))
            );
        }

        return new ProductMatchResult(
                null,
                rankedMatches.stream()
                        .limit(5)
                        .map(ScoredProductMatch::product)
                        .map(this::toProductCandidate)
                        .toList()
        );
    }

    private DiseaseMatchResult findDiseaseCandidates(String rawName) {
        Optional<DiseaseCode> exactMatch = diseaseCodeRepository.findByDiseaseNameAndDeletedAtIsNull(rawName);
        if (exactMatch.isPresent()) {
            DiseaseCode diseaseCode = exactMatch.get();
            return new DiseaseMatchResult(
                    diseaseCode.getDiseaseCode(),
                    List.of(toDiseaseCandidate(diseaseCode))
            );
        }

        List<DiseaseCode> containsMatches =
                diseaseCodeRepository.findTop10ByDiseaseNameContainingAndDeletedAtIsNullOrderByDiseaseNameAsc(rawName);
        List<DiseaseCode> filtered = containsMatches.stream()
                .filter(diseaseCode -> isSimilar(normalizeText(diseaseCode.getDiseaseName()), normalizeText(rawName)))
                .toList();

        if (filtered.size() == 1) {
            DiseaseCode diseaseCode = filtered.get(0);
            return new DiseaseMatchResult(
                    diseaseCode.getDiseaseCode(),
                    List.of(toDiseaseCandidate(diseaseCode))
            );
        }

        return new DiseaseMatchResult(null, filtered.stream().map(this::toDiseaseCandidate).toList());
    }

    private ConsultationAiResolutionResponse.Candidate toContractCandidate(CustomerOwnedContractResponse contract) {
        return ConsultationAiResolutionResponse.Candidate.builder()
                .id(contract.getContractId().toString())
                .code(null)
                .label(contract.getInsuranceProductName())
                .subLabel(contract.getInsuranceCompanyName() + " / " + contract.getContractStatus())
                .build();
    }

    private ConsultationAiResolutionResponse.Candidate toProductCandidate(InsuranceProduct product) {
        return ConsultationAiResolutionResponse.Candidate.builder()
                .id(product.getId().toString())
                .code(product.getInsuranceProductCode())
                .label(product.getInsuranceProductName())
                .subLabel(product.getInsuranceCompany() != null
                        ? product.getInsuranceCompany().getInsuranceCompanyName()
                        : null)
                .build();
    }

    private ConsultationAiResolutionResponse.Candidate toDiseaseCandidate(DiseaseCode diseaseCode) {
        return ConsultationAiResolutionResponse.Candidate.builder()
                .id(diseaseCode.getId().toString())
                .code(diseaseCode.getDiseaseCode())
                .label(diseaseCode.getDiseaseName())
                .subLabel(diseaseCode.getDiseaseCategory())
                .build();
    }

    private boolean matchesContractHint(CustomerOwnedContractResponse contract, String hint) {
        String normalizedHint = normalizeText(hint);
        String productName = normalizeText(contract.getInsuranceProductName());
        String companyAndProduct = normalizeText(contract.getInsuranceCompanyName() + " " + contract.getInsuranceProductName());

        return isSimilar(productName, normalizedHint) || isSimilar(companyAndProduct, normalizedHint);
    }

    private boolean isSimilar(String source, String target) {
        if (source == null || source.isBlank() || target == null || target.isBlank()) {
            return false;
        }
        return source.equals(target) || source.contains(target) || target.contains(source);
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

    private void backfillAiHintsFromReferenceText(ConsultationAiStructuredDraft draft, String referenceText) {
        if (draft == null || referenceText == null || referenceText.isBlank()) {
            return;
        }
        if (draft.getConsultationType() != ConsultationType.NEW_CONTRACT) {
            return;
        }
        if (draft.getNewDetail() != null
                && draft.getNewDetail().getProposedProductCodes() != null
                && !draft.getNewDetail().getProposedProductCodes().isEmpty()) {
            return;
        }

        List<String> extractedHints = extractProductHints(referenceText);
        if (extractedHints.isEmpty()) {
            return;
        }

        ConsultationAiStructuredDraft.AiHints aiHints = draft.getAiHints();
        if (aiHints == null) {
            aiHints = new ConsultationAiStructuredDraft.AiHints();
            draft.setAiHints(aiHints);
        }

        LinkedHashSet<String> mergedHints = new LinkedHashSet<>();
        if (aiHints.getMentionedProductNames() != null) {
            mergedHints.addAll(aiHints.getMentionedProductNames());
        }
        mergedHints.addAll(extractedHints);
        aiHints.setMentionedProductNames(new ArrayList<>(mergedHints));
    }

    private List<String> extractProductHints(String referenceText) {
        LinkedHashSet<String> matches = new LinkedHashSet<>();
        Matcher matcher = PRODUCT_HINT_PATTERN.matcher(referenceText);
        while (matcher.find()) {
            String candidate = sanitizeProductHintCandidate(matcher.group(1));
            if (candidate != null) {
                matches.add(candidate);
            }
        }
        return new ArrayList<>(matches);
    }

    private String sanitizeProductHintCandidate(String candidate) {
        if (candidate == null) {
            return null;
        }

        String normalized = candidate.trim().replaceAll("\\s+", " ");
        for (String prefix : PRODUCT_HINT_PREFIXES) {
            if (normalized.startsWith(prefix)) {
                normalized = normalized.substring(prefix.length()).trim();
            }
        }

        String compact = normalized.replace(" ", "");
        if (compact.length() < 3) {
            return null;
        }
        if (!(compact.contains("보험") || compact.contains("보장") || compact.contains("특약"))) {
            return null;
        }

        return normalized;
    }

    private void normalizeAiHints(ConsultationAiStructuredDraft draft) {
        if (draft == null || draft.getAiHints() == null) {
            return;
        }

        ConsultationAiStructuredDraft.AiHints aiHints = draft.getAiHints();
        aiHints.setTargetContractHint(blankToNull(aiHints.getTargetContractHint()));
        aiHints.setMentionedProductNames(normalizeHintValues(aiHints.getMentionedProductNames()));
        aiHints.setMentionedDiseaseNames(normalizeHintValues(aiHints.getMentionedDiseaseNames()));
        aiHints.setClaimTypeHint(blankToNull(aiHints.getClaimTypeHint()));
        aiHints.setClaimReviewItemHints(normalizeHintValues(aiHints.getClaimReviewItemHints()));
        aiHints.setClaimResultHint(blankToNull(aiHints.getClaimResultHint()));
        aiHints.setClaimNextActionHints(normalizeHintValues(aiHints.getClaimNextActionHints()));
        aiHints.setRenewalConsultationResultHint(blankToNull(aiHints.getRenewalConsultationResultHint()));
        aiHints.setRenewalCoverageChangeTypeHint(blankToNull(aiHints.getRenewalCoverageChangeTypeHint()));
        aiHints.setRenewalCustomerReactionHint(blankToNull(aiHints.getRenewalCustomerReactionHint()));
        aiHints.setRenewalInterestTypeHints(normalizeHintValues(aiHints.getRenewalInterestTypeHints()));
        aiHints.setRenewalPremiumChangeReasonHints(normalizeHintValues(aiHints.getRenewalPremiumChangeReasonHints()));
        aiHints.setRenewalNextActionHint(blankToNull(aiHints.getRenewalNextActionHint()));
        aiHints.setTerminationReasonHints(normalizeHintValues(aiHints.getTerminationReasonHints()));
        aiHints.setTerminationRetentionPossibilityHint(blankToNull(aiHints.getTerminationRetentionPossibilityHint()));

        if (aiHints.getTargetContractHint() == null
                && aiHints.getMentionedProductNames() == null
                && aiHints.getMentionedDiseaseNames() == null
                && aiHints.getClaimTypeHint() == null
                && aiHints.getClaimReviewItemHints() == null
                && aiHints.getClaimResultHint() == null
                && aiHints.getClaimNextActionHints() == null
                && aiHints.getRenewalConsultationResultHint() == null
                && aiHints.getRenewalCoverageChangeTypeHint() == null
                && aiHints.getRenewalCustomerReactionHint() == null
                && aiHints.getRenewalInterestTypeHints() == null
                && aiHints.getRenewalPremiumChangeReasonHints() == null
                && aiHints.getRenewalNextActionHint() == null
                && aiHints.getTerminationReasonHints() == null
                && aiHints.getTerminationRetentionPossibilityHint() == null) {
            draft.setAiHints(null);
        }
    }

    private List<String> normalizeHintValues(List<String> values) {
        if (values == null) {
            return null;
        }

        List<String> normalized = values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();

        return normalized.isEmpty() ? null : normalized;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private List<InsuranceProduct> deduplicateProducts(List<InsuranceProduct> products) {
        Map<String, InsuranceProduct> unique = new LinkedHashMap<>();
        for (InsuranceProduct product : products) {
            unique.putIfAbsent(product.getInsuranceProductCode(), product);
        }
        return new ArrayList<>(unique.values());
    }

    private List<InsuranceProduct> searchProductCandidates(String rawName) {
        Map<String, InsuranceProduct> unique = new LinkedHashMap<>();
        addProductMatches(unique, rawName);
        for (String keyword : extractProductSearchKeywords(rawName)) {
            addProductMatches(unique, keyword);
        }
        return new ArrayList<>(unique.values());
    }

    private void addProductMatches(Map<String, InsuranceProduct> unique, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        List<InsuranceProduct> matches =
                insuranceProductRepository.findTop10ByInsuranceProductNameContainingAndDeletedAtIsNullOrderByInsuranceProductNameAsc(keyword);
        for (InsuranceProduct match : matches) {
            unique.putIfAbsent(match.getInsuranceProductCode(), match);
        }
    }

    private List<String> extractProductSearchKeywords(String rawName) {
        String trimmed = rawName == null ? null : rawName.trim();
        if (trimmed == null || trimmed.isBlank()) {
            return List.of();
        }

        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        for (String token : PRODUCT_KEYWORD_SPLIT_PATTERN.split(trimmed)) {
            String normalizedToken = token.trim();
            if (normalizedToken.length() < 2) {
                continue;
            }
            if (normalizedToken.chars().allMatch(Character::isDigit)) {
                continue;
            }
            if (PRODUCT_SEARCH_STOPWORDS.contains(normalizedToken)) {
                continue;
            }
            keywords.add(normalizedToken);
        }
        return new ArrayList<>(keywords);
    }

    private int scoreProductMatch(InsuranceProduct product, String rawName) {
        String normalizedProductName = normalizeText(product.getInsuranceProductName());
        String normalizedRawName = normalizeText(rawName);
        if (normalizedProductName == null || normalizedRawName == null) {
            return 0;
        }

        if (normalizedProductName.equals(normalizedRawName)) {
            return 1_000;
        }

        int score = 0;
        if (normalizedProductName.contains(normalizedRawName) || normalizedRawName.contains(normalizedProductName)) {
            score += 200;
        }

        String productCore = normalizeProductCore(product.getInsuranceProductName());
        String rawCore = normalizeProductCore(rawName);
        if (productCore != null && rawCore != null && !productCore.isBlank() && !rawCore.isBlank()) {
            int distance = levenshteinDistance(productCore, rawCore);
            int maxLength = Math.max(productCore.length(), rawCore.length());
            int similarityPercent = maxLength == 0 ? 0 : ((maxLength - distance) * 100) / maxLength;
            if (similarityPercent >= 80) {
                score += 160;
            } else if (similarityPercent >= 65) {
                score += 100;
            }
        }

        for (String keyword : extractProductSearchKeywords(rawName)) {
            String normalizedKeyword = normalizeText(keyword);
            if (normalizedKeyword != null && normalizedProductName.contains(normalizedKeyword)) {
                score += 40;
            }
        }

        String inferredCode = resolveProductCode(rawName);
        if (inferredCode != null && inferredCode.equals(product.getInsuranceProductCode())) {
            score += 500;
        }

        return score;
    }

    private boolean shouldAutoMap(List<ScoredProductMatch> rankedMatches) {
        if (rankedMatches.isEmpty()) {
            return false;
        }
        if (rankedMatches.size() == 1) {
            return true;
        }

        ScoredProductMatch top = rankedMatches.get(0);
        ScoredProductMatch second = rankedMatches.get(1);
        return top.score() >= 250 && top.score() - second.score() >= 80;
    }

    private String normalizeProductCore(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        return normalized
                .replace("보험", "")
                .replace("보장", "")
                .replace("특약", "")
                .replace("플랜", "")
                .replace("상품", "");
    }

    private int levenshteinDistance(String left, String right) {
        int[][] dp = new int[left.length() + 1][right.length() + 1];

        for (int i = 0; i <= left.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= right.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[left.length()][right.length()];
    }

    private String normalizeText(String value) {
        return value == null ? null : value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private record NormalizationResult(
            ConsultationAiStructuredDraft draft,
            List<String> warnings
    ) {
    }

    private record DraftResolution(
            ConsultationAiStructuredDraft draft,
            ConsultationAiResolutionResponse resolution
    ) {
    }

    private record ProductMatchResult(
            String autoMappedCode,
            List<ConsultationAiResolutionResponse.Candidate> candidates
    ) {
    }

    private record DiseaseMatchResult(
            String autoMappedCode,
            List<ConsultationAiResolutionResponse.Candidate> candidates
    ) {
    }

    private record ScoredProductMatch(
            InsuranceProduct product,
            int score
    ) {
    }
}
