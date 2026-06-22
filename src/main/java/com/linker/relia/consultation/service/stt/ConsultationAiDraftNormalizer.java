package com.linker.relia.consultation.service.stt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.consultation.domain.ConsultationNewCoverageType;
import com.linker.relia.consultation.domain.ConsultationType;
import com.linker.relia.consultation.domain.stt.ConsultationSttSession;
import com.linker.relia.consultation.dto.response.ConsultationAiStructuredDraft;
import com.linker.relia.consultation.exception.ConsultationErrorCode;
import com.linker.relia.insurance.domain.InsuranceProduct;
import com.linker.relia.insurance.repository.InsuranceProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationAiDraftNormalizer {
    private static final Pattern PRODUCT_CODE_PATTERN =
            Pattern.compile("\\bLP\\s*0*(\\d{1,3})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRAILING_NUMBER_PATTERN = Pattern.compile("(\\d{1,3})\\s*$");
    private static final Pattern PRODUCT_HINT_PATTERN = Pattern.compile(
            "([가-힣A-Za-z0-9]+(?:\\s+[가-힣A-Za-z0-9]+){0,5}\\s*(?:보험|보장|특약)(?:\\s*본)?(?:\\s*\\d{1,3})?)"
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

    private final InsuranceProductRepository insuranceProductRepository;
    private final ObjectMapper objectMapper;

    public ConsultationAiStructuredDraft enrichStructuredDraft(
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

    public ConsultationAiDraftNormalizationResult normalizeStoredStructuredData(String rawJson, String referenceText) {
        if (rawJson == null || rawJson.isBlank()) {
            return new ConsultationAiDraftNormalizationResult(null, List.of());
        }

        try {
            ConsultationAiStructuredDraft draft = objectMapper.readValue(rawJson, ConsultationAiStructuredDraft.class);
            return normalizeWithWarnings(draft, referenceText);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_INVALID_DATA);
        }
    }

    public ConsultationAiDraftNormalizationResult normalizeWithWarnings(
            ConsultationAiStructuredDraft draft,
            String referenceText
    ) {
        if (draft == null) {
            return new ConsultationAiDraftNormalizationResult(null, List.of());
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
        return new ConsultationAiDraftNormalizationResult(draft, List.copyOf(warnings));
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
        if (containsAny(compact, "보험료", "저렴", "가격", "비용", "가성비", "월입부담")) {
            return "보험료";
        }
        if (containsAny(compact, "보장", "보장범위", "보장넓이", "보장두께")) {
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
        if (containsAny(compact, "보험료", "저렴", "가격", "비용", "가성비", "월입부담")) {
            return "보험료";
        }
        if (containsAny(compact, "보장", "보장범위", "보장넓이", "보장두께")) {
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

    String resolveProductCode(String rawValue) {
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

    void normalizeAiHints(ConsultationAiStructuredDraft draft) {
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

    String normalizeText(String value) {
        return value == null ? null : value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
