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

import java.math.BigDecimal;
import java.math.RoundingMode;
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
            Pattern.compile("\\b([A-Z]{2,4}\\d{3})\\b", Pattern.CASE_INSENSITIVE);
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
        boolean handled = true;
        if (handled) {
            normalizeCommonFields(draft, warnings);
            if (draft.getConsultationType() == ConsultationType.NEW_CONTRACT && draft.getNewDetail() != null) {
                normalizeNewDetail(draft.getNewDetail(), warnings, referenceText, draft);
                draft.setClaimDetail(null);
                draft.setRenewalDetail(null);
                draft.setCancelDetail(null);
            } else if (draft.getConsultationType() == ConsultationType.CLAIM && draft.getClaimDetail() != null) {
                normalizeClaimDetail(draft.getClaimDetail(), warnings);
                draft.setCustomerInfo(null);
                draft.setNewDetail(null);
                draft.setRenewalDetail(null);
                draft.setCancelDetail(null);
            } else if (draft.getConsultationType() == ConsultationType.RENEWAL && draft.getRenewalDetail() != null) {
                normalizeRenewalDetail(draft.getRenewalDetail(), warnings);
                draft.setCustomerInfo(null);
                draft.setNewDetail(null);
                draft.setClaimDetail(null);
                draft.setCancelDetail(null);
            } else if (draft.getConsultationType() == ConsultationType.TERMINATION && draft.getCancelDetail() != null) {
                normalizeCancelDetail(draft.getCancelDetail(), warnings);
                draft.setCustomerInfo(null);
                draft.setNewDetail(null);
                draft.setClaimDetail(null);
                draft.setRenewalDetail(null);
            }

            backfillAiHintsFromReferenceText(draft, referenceText);
            normalizeAiHints(draft);
            return new ConsultationAiDraftNormalizationResult(draft, List.copyOf(warnings));
        }

        if (draft.getConsultationType() == ConsultationType.NEW_CONTRACT && draft.getCustomerId() != null) {
            if (draft.getCustomerInfo() != null) {
                warnings.add("customerId가 존재해 customerInfo는 제거했습니다.");
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
                    warnings.add("coverageTypes의 상품성 값 '" + rawValue
                            + "'를 proposedProductCodes의 " + candidateProductCode + "(으)로 이동했습니다.");
                    log.info("Normalized invalid coverageTypes value to proposedProductCodes. field=coverageTypes rawValue={} normalizedCode={}",
                            rawValue, candidateProductCode);
                    continue;
                }

                warnings.add("coverageTypes의 지원하지 않는 값 '" + rawValue + "'를 제거했습니다.");
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

                warnings.add("proposedProductCodes의 코드가 아닌 값 '" + rawValue + "'를 제거했습니다.");
                log.warn("Dropped invalid structured draft value. field=proposedProductCodes rawValue={}", rawValue);
            }
        }

        newDetail.setCoverageTypes(toNullableList(normalizedCoverageTypes));
        newDetail.setProposedProductCodes(toNullableList(normalizedProductCodes));
        normalizeInsurancePriority(newDetail, referenceText, warnings);
    }

    private void normalizeNewDetail(
            ConsultationAiStructuredDraft.NewDetail newDetail,
            List<String> warnings,
            String referenceText,
            ConsultationAiStructuredDraft draft
    ) {
        normalizeNewDetail(newDetail, warnings, referenceText);
        newDetail.setExistingInsuranceNote(blankToNull(newDetail.getExistingInsuranceNote()));
        newDetail.setInsurancePriority(blankToNull(newDetail.getInsurancePriority()));

        if (newDetail.getProposedProductCodes() != null) {
            Set<String> normalizedProductCodes = new LinkedHashSet<>();
            for (String rawValue : newDetail.getProposedProductCodes()) {
                String candidateProductCode = resolveProductCode(rawValue);
                if (candidateProductCode != null) {
                    normalizedProductCodes.add(candidateProductCode);
                } else if (rawValue != null && !rawValue.isBlank()) {
                    addMentionedProductHint(draft, rawValue);
                    warnings.add("상품 코드가 확정되지 않은 값 '" + rawValue + "' 는 aiHints 로 이동했습니다.");
                }
            }
            newDetail.setProposedProductCodes(toNullableList(normalizedProductCodes));
        }
    }

    private void normalizeInsurancePriority(
            ConsultationAiStructuredDraft.NewDetail newDetail,
            String referenceText,
            List<String> warnings
    ) {
        if (newDetail.getInsurancePriority() != null && !newDetail.getInsurancePriority().isBlank()) {
            String normalized = mapInsurancePriority(newDetail.getInsurancePriority());
            if (!Objects.equals(normalized, newDetail.getInsurancePriority())) {
                warnings.add("insurancePriority를 '" + normalized + "'(으)로 정규화했습니다.");
            }
            newDetail.setInsurancePriority(normalized);
            return;
        }

        String inferred = inferInsurancePriority(referenceText);
        if (inferred != null) {
            newDetail.setInsurancePriority(inferred);
            warnings.add("insurancePriority를 전사 내용 기준으로 '" + inferred + "'(으)로 추론했습니다.");
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
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String trimmed = rawValue.trim();
        Matcher codeMatcher = PRODUCT_CODE_PATTERN.matcher(trimmed);
        if (codeMatcher.find()) {
            return codeMatcher.group(1).toUpperCase(Locale.ROOT);
        }

        Optional<InsuranceProduct> exactMatch = insuranceProductRepository
                .findByInsuranceProductNameAndDeletedAtIsNull(trimmed);
        return exactMatch.map(InsuranceProduct::getInsuranceProductCode).orElse(null);
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
            String normalizedKeyword = normalizeText(keyword);
            if (normalizedKeyword != null && source.contains(normalizedKeyword)) {
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

    private void normalizeCommonFields(ConsultationAiStructuredDraft draft, List<String> warnings) {
        draft.setConsultationContent(blankToNull(draft.getConsultationContent()));
        draft.setSpecialNote(blankToNull(draft.getSpecialNote()));
        if (draft.getConsultationContent() == null && draft.getSpecialNote() != null) {
            draft.setConsultationContent(draft.getSpecialNote());
        }
        if (draft.getSpecialNote() == null && draft.getConsultationContent() != null) {
            draft.setSpecialNote(draft.getConsultationContent());
        }

        if (draft.getConsultationType() == ConsultationType.NEW_CONTRACT
                && draft.getCustomerId() != null
                && draft.getCustomerInfo() != null) {
            warnings.add("customerId가 존재하여 customerInfo는 null로 정리했습니다.");
            draft.setCustomerInfo(null);
        }
    }

    private void normalizeClaimDetail(
            ConsultationAiStructuredDraft.ClaimDetail claimDetail,
            List<String> warnings
    ) {
        claimDetail.setClaimType(blankToNull(claimDetail.getClaimType()));
        claimDetail.setClaimReason(blankToNull(claimDetail.getClaimReason()));
        claimDetail.setHospitalName(blankToNull(claimDetail.getHospitalName()));
        claimDetail.setDiagnosisOrTreatment(blankToNull(claimDetail.getDiagnosisOrTreatment()));
        claimDetail.setHospitalizationStatus(normalizeYnValue(
                claimDetail.getHospitalizationStatus(),
                warnings,
                "claimDetail.hospitalizationStatus"
        ));
        claimDetail.setSurgeryStatus(normalizeYnValue(
                claimDetail.getSurgeryStatus(),
                warnings,
                "claimDetail.surgeryStatus"
        ));
        claimDetail.setReviewItems(normalizeStringList(claimDetail.getReviewItems()));
        claimDetail.setResult(blankToNull(claimDetail.getResult()));
        claimDetail.setNextActions(normalizeStringList(claimDetail.getNextActions()));
    }

    private void normalizeRenewalDetail(
            ConsultationAiStructuredDraft.RenewalDetail renewalDetail,
            List<String> warnings
    ) {
        renewalDetail.setRenewalReason(blankToNull(renewalDetail.getRenewalReason()));
        renewalDetail.setCoverageChangeType(blankToNull(renewalDetail.getCoverageChangeType()));
        renewalDetail.setCoverageChangeDetail(blankToNull(renewalDetail.getCoverageChangeDetail()));
        renewalDetail.setCustomerReaction(blankToNull(renewalDetail.getCustomerReaction()));
        renewalDetail.setConsultationResult(blankToNull(renewalDetail.getConsultationResult()));
        renewalDetail.setOtherReason(blankToNull(renewalDetail.getOtherReason()));
        renewalDetail.setInterestTypes(normalizeStringList(renewalDetail.getInterestTypes()));
        renewalDetail.setPremiumChangeReasonTypes(normalizeStringList(renewalDetail.getPremiumChangeReasonTypes()));
        renewalDetail.setNextActions(normalizeStringList(renewalDetail.getNextActions()));

        if (renewalDetail.getCurrentPremium() != null
                && renewalDetail.getRenewalPremium() != null
                && renewalDetail.getCurrentPremium() > 0) {
            BigDecimal current = BigDecimal.valueOf(renewalDetail.getCurrentPremium());
            BigDecimal renewed = BigDecimal.valueOf(renewalDetail.getRenewalPremium());
            BigDecimal changeRate = renewed.subtract(current)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(current, 1, RoundingMode.HALF_UP);
            renewalDetail.setPremiumChangeRate(changeRate);
        } else if (renewalDetail.getPremiumChangeRate() != null) {
            renewalDetail.setPremiumChangeRate(renewalDetail.getPremiumChangeRate().setScale(1, RoundingMode.HALF_UP));
        } else if (renewalDetail.getCurrentPremium() == null || renewalDetail.getRenewalPremium() == null) {
            warnings.add("currentPremium 또는 renewalPremium이 없어 premiumChangeRate를 재계산하지 못했습니다.");
        }
    }

    private void normalizeCancelDetail(
            ConsultationAiStructuredDraft.CancelDetail cancelDetail,
            List<String> warnings
    ) {
        cancelDetail.setReviewReasons(normalizeStringList(cancelDetail.getReviewReasons()));
        cancelDetail.setReasonDetail(blankToNull(cancelDetail.getReasonDetail()));
        cancelDetail.setRetentionPlans(normalizeStringList(cancelDetail.getRetentionPlans()));
        cancelDetail.setCustomerIntent(blankToNull(cancelDetail.getCustomerIntent()));
        cancelDetail.setResult(blankToNull(cancelDetail.getResult()));
        cancelDetail.setNextActions(normalizeStringList(cancelDetail.getNextActions()));
        cancelDetail.setRetentionPossibility(normalizeRetentionPossibility(cancelDetail.getRetentionPossibility(), warnings));

        Set<String> normalizedReasons = new LinkedHashSet<>();
        if (cancelDetail.getReviewReasons() != null) {
            normalizedReasons.addAll(cancelDetail.getReviewReasons());
        }
        String normalizedReasonDetail = normalizeText(cancelDetail.getReasonDetail());
        if (normalizedReasonDetail != null) {
            if (normalizedReasonDetail.contains("보험료")) {
                normalizedReasons.add("보험료 부담");
            }
            if (normalizedReasonDetail.contains("중복")) {
                normalizedReasons.add("중복 보장");
            }
            if (normalizedReasonDetail.contains("보장") && normalizedReasonDetail.contains("불만")) {
                normalizedReasons.add("보장 불만족");
            }
        }
        cancelDetail.setReviewReasons(normalizedReasons.isEmpty() ? null : new ArrayList<>(normalizedReasons));

        cancelDetail.setPremiumBurden(fillFlag(cancelDetail.getPremiumBurden(), normalizedReasons, "보험료"));
        cancelDetail.setRenewalPremiumBurden(fillFlag(cancelDetail.getRenewalPremiumBurden(), normalizedReasons, "갱신"));
        cancelDetail.setPaymentDifficulty(fillFlag(cancelDetail.getPaymentDifficulty(), normalizedReasons, "납입"));
        cancelDetail.setCoverageDissatisfaction(fillFlag(cancelDetail.getCoverageDissatisfaction(), normalizedReasons, "보장"));
        cancelDetail.setDuplicateCoverage(fillFlag(cancelDetail.getDuplicateCoverage(), normalizedReasons, "중복"));
    }

    private Boolean fillFlag(Boolean current, Set<String> reasons, String keyword) {
        if (current != null) {
            return current;
        }
        for (String reason : reasons) {
            String normalized = normalizeText(reason);
            if (normalized != null && normalized.contains(normalizeText(keyword))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeYnValue(String rawValue, List<String> warnings, String fieldPath) {
        String normalized = blankToNull(rawValue);
        if (normalized == null) {
            return null;
        }

        String compact = normalizeText(normalized);
        if (compact.equals("y") || compact.equals("yes") || compact.equals("있음") || compact.equals("입원") || compact.equals("수술")) {
            return "Y";
        }
        if (compact.equals("n") || compact.equals("no") || compact.equals("없음") || compact.equals("없다")) {
            return "N";
        }

        warnings.add(fieldPath + " 값을 Y/N으로 확정하지 못해 null로 정리했습니다.");
        return null;
    }

    private String normalizeRetentionPossibility(String rawValue, List<String> warnings) {
        String normalized = blankToNull(rawValue);
        if (normalized == null) {
            return null;
        }

        String compact = normalizeText(normalized);
        if (compact.contains("high") || compact.contains("높")) {
            return "HIGH";
        }
        if (compact.contains("medium") || compact.contains("보통") || compact.equals("중")) {
            return "MEDIUM";
        }
        if (compact.contains("low") || compact.contains("낮")) {
            return "LOW";
        }

        warnings.add("retentionPossibility 값을 HIGH/MEDIUM/LOW로 확정하지 못해 null로 정리했습니다.");
        return null;
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
        aiHints.setClaimHospitalizationStatusHint(blankToNull(aiHints.getClaimHospitalizationStatusHint()));
        aiHints.setClaimSurgeryStatusHint(blankToNull(aiHints.getClaimSurgeryStatusHint()));
        aiHints.setRenewalConsultationResultHint(blankToNull(aiHints.getRenewalConsultationResultHint()));
        aiHints.setRenewalCoverageChangeTypeHint(blankToNull(aiHints.getRenewalCoverageChangeTypeHint()));
        aiHints.setRenewalCustomerReactionHint(blankToNull(aiHints.getRenewalCustomerReactionHint()));
        aiHints.setRenewalInterestTypeHints(normalizeHintValues(aiHints.getRenewalInterestTypeHints()));
        aiHints.setRenewalPremiumChangeReasonTypeHints(normalizeHintValues(aiHints.getRenewalPremiumChangeReasonTypeHints()));
        aiHints.setRenewalNextActionHints(normalizeHintValues(aiHints.getRenewalNextActionHints()));
        aiHints.setCancellationReviewReasonHints(normalizeHintValues(aiHints.getCancellationReviewReasonHints()));
        aiHints.setCancellationRetentionPlanHints(normalizeHintValues(aiHints.getCancellationRetentionPlanHints()));
        aiHints.setCancellationCustomerIntentHint(blankToNull(aiHints.getCancellationCustomerIntentHint()));
        aiHints.setCancellationResultHint(blankToNull(aiHints.getCancellationResultHint()));
        aiHints.setCancellationNextActionHints(normalizeHintValues(aiHints.getCancellationNextActionHints()));
        aiHints.setTerminationRetentionPossibilityHint(blankToNull(aiHints.getTerminationRetentionPossibilityHint()));

        if (aiHints.getTargetContractHint() == null
                && aiHints.getMentionedProductNames() == null
                && aiHints.getMentionedDiseaseNames() == null
                && aiHints.getClaimTypeHint() == null
                && aiHints.getClaimReviewItemHints() == null
                && aiHints.getClaimResultHint() == null
                && aiHints.getClaimNextActionHints() == null
                && aiHints.getClaimHospitalizationStatusHint() == null
                && aiHints.getClaimSurgeryStatusHint() == null
                && aiHints.getRenewalConsultationResultHint() == null
                && aiHints.getRenewalCoverageChangeTypeHint() == null
                && aiHints.getRenewalCustomerReactionHint() == null
                && aiHints.getRenewalInterestTypeHints() == null
                && aiHints.getRenewalPremiumChangeReasonTypeHints() == null
                && aiHints.getRenewalNextActionHints() == null
                && aiHints.getCancellationReviewReasonHints() == null
                && aiHints.getCancellationRetentionPlanHints() == null
                && aiHints.getCancellationCustomerIntentHint() == null
                && aiHints.getCancellationResultHint() == null
                && aiHints.getCancellationNextActionHints() == null
                && aiHints.getTerminationRetentionPossibilityHint() == null) {
            draft.setAiHints(null);
        }
    }

    private void addMentionedProductHint(ConsultationAiStructuredDraft draft, String rawValue) {
        ConsultationAiStructuredDraft.AiHints aiHints = draft.getAiHints();
        if (aiHints == null) {
            aiHints = new ConsultationAiStructuredDraft.AiHints();
            draft.setAiHints(aiHints);
        }

        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (aiHints.getMentionedProductNames() != null) {
            values.addAll(aiHints.getMentionedProductNames());
        }
        values.add(rawValue.trim());
        aiHints.setMentionedProductNames(new ArrayList<>(values));
    }

    private List<String> normalizeStringList(List<String> values) {
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

    private List<String> normalizeHintValues(List<String> values) {
        return normalizeStringList(values);
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
