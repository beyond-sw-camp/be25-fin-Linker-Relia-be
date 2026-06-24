package com.linker.relia.consultation.service.stt;

import com.linker.relia.consultation.domain.ConsultationType;
import com.linker.relia.consultation.domain.stt.ConsultationSttSession;
import com.linker.relia.consultation.dto.response.ConsultationAiResolutionResponse;
import com.linker.relia.consultation.dto.response.ConsultationAiStructuredDraft;
import com.linker.relia.contract.repository.ContractRepository;
import com.linker.relia.customer.domain.DiseaseCode;
import com.linker.relia.customer.dto.CustomerOwnedContractResponse;
import com.linker.relia.customer.repository.DiseaseCodeRepository;
import com.linker.relia.insurance.domain.InsuranceProduct;
import com.linker.relia.insurance.repository.InsuranceProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ConsultationAiResolutionService {
    private static final String STATUS_AUTO_MAPPED = "AUTO_MAPPED";
    private static final String STATUS_NEEDS_USER_CONFIRMATION = "NEEDS_USER_CONFIRMATION";
    private static final String STATUS_NO_CANDIDATE = "NO_CANDIDATE";

    private static final Pattern PRODUCT_KEYWORD_SPLIT_PATTERN = Pattern.compile("[\\s,/()\\[\\]{}]+");
    private static final Set<String> PRODUCT_SEARCH_STOPWORDS = Set.of(
            "보험", "보장", "특약", "상품", "플랜", "추천", "가입", "설계", "상담", "고객", "보장형"
    );

    private static final List<ChoiceOption> CLAIM_TYPE_OPTIONS = List.of(
            choice("DISEASE", "질병", "질병청구", "암", "진단"),
            choice("INJURY", "상해", "상해청구", "사고"),
            choice("ETC", "기타")
    );
    private static final List<ChoiceOption> CLAIM_REVIEW_ITEM_OPTIONS = List.of(
            choice("서류 확인", "서류 확인", "서류", "증빙"),
            choice("면책 확인", "면책 확인", "면책"),
            choice("고지의무 확인", "고지의무 확인", "고지", "고지의무"),
            choice("보장내용 확인", "보장내용 확인", "보장", "담보", "보장내용")
    );
    private static final List<ChoiceOption> YN_OPTIONS = List.of(
            choice("Y", "예", "네", "있음", "있다", "맞음", "yes", "true", "y"),
            choice("N", "아니오", "아니요", "없음", "없다", "아님", "no", "false", "n")
    );
    private static final List<ChoiceOption> COVERAGE_TYPE_OPTIONS = List.of(
            choice("CANCER", "암", "암보장"),
            choice("HEART", "심장", "심혈관", "뇌심장"),
            choice("LIFE", "종신", "사망", "생명"),
            choice("LONG_TERM_CARE", "간병", "장기요양", "치매")
    );
    private static final List<ChoiceOption> GENDER_OPTIONS = List.of(
            choice("MALE", "남성", "남자", "남", "m", "male"),
            choice("FEMALE", "여성", "여자", "여", "f", "female")
    );
    private static final List<ChoiceOption> MARITAL_STATUS_OPTIONS = List.of(
            choice("SINGLE", "미혼", "싱글"),
            choice("MARRIED", "기혼", "결혼"),
            choice("DIVORCED", "이혼"),
            choice("BEREAVED", "사별")
    );
    private static final List<ChoiceOption> COVERAGE_CHANGE_TYPE_OPTIONS = List.of(
            choice("INCREASE", "증액", "보장증가", "늘림"),
            choice("DECREASE", "감액", "보장축소", "줄임"),
            choice("MAINTAIN", "유지", "변경없음"),
            choice("NEW", "신규추가", "추가가입", "특약추가")
    );
    private static final List<ChoiceOption> PREMIUM_CHANGE_REASON_TYPE_OPTIONS = List.of(
            choice("AGE", "연령", "나이"),
            choice("LOSS_RATIO", "손해율"),
            choice("CLAIM_HISTORY", "청구이력", "사고이력"),
            choice("COVERAGE_CHANGE", "보장변경", "담보변경"),
            choice("DISCOUNT_END", "할인종료", "할인만료"),
            choice("ETC", "기타")
    );
    private static final List<ChoiceOption> CUSTOMER_REACTION_OPTIONS = List.of(
            choice("긍정적", "긍정적", "긍정", "좋음", "수용"),
            choice("보통", "보통", "중립", "검토"),
            choice("부정적", "부정적", "부정", "거절", "부담")
    );
    private static final List<ChoiceOption> CONSULTATION_RESULT_OPTIONS = List.of(
            choice("완료", "완료", "상담완료"),
            choice("보류", "보류", "검토중"),
            choice("재안내 예정", "재안내 예정", "재안내", "추후안내", "후속상담"),
            choice("거절", "거절", "거부")
    );
    private static final List<ChoiceOption> CANCELLATION_REVIEW_REASON_OPTIONS = List.of(
            choice("보험료 부담", "보험료 부담", "보험료부담", "보험료", "비쌈", "부담"),
            choice("보장 불만족", "보장 불만족", "보장불만", "보장부족", "보장불만족"),
            choice("중복 보장", "중복 보장", "중복", "보장중복"),
            choice("납입 부담", "납입 부담", "납입", "납입곤란", "납입부담"),
            choice("기타", "기타")
    );
    private static final List<ChoiceOption> RETENTION_PLAN_OPTIONS = List.of(
            choice("보험료 조정", "보험료 조정", "보험료조정", "감액", "조정"),
            choice("대안 상품 제안", "대안 상품 제안", "대안상품", "대안", "상품비교", "비교안"),
            choice("보장 재설계", "보장 재설계", "재설계", "보장재설계"),
            choice("재상담", "재상담", "다시상담", "재안내")
    );
    private static final List<ChoiceOption> CUSTOMER_INTENT_OPTIONS = List.of(
            choice("해지 희망", "해지 희망", "해지", "해지희망"),
            choice("유지 희망", "유지 희망", "유지", "유지희망"),
            choice("검토 후 결정", "검토 후 결정", "검토", "고민", "추후결정")
    );
    private static final List<ChoiceOption> RETENTION_POSSIBILITY_OPTIONS = List.of(
            choice("HIGH", "높음", "상", "high"),
            choice("MEDIUM", "보통", "중", "medium"),
            choice("LOW", "낮음", "하", "low")
    );
    private static final List<ChoiceOption> BOOLEAN_OPTIONS = List.of(
            choice("true", "예", "네", "있음", "있다", "가입", "보유", "true", "yes", "y"),
            choice("false", "아니오", "아니요", "없음", "없다", "미가입", "미보유", "false", "no", "n")
    );

    private final InsuranceProductRepository insuranceProductRepository;
    private final ContractRepository contractRepository;
    private final DiseaseCodeRepository diseaseCodeRepository;
    private final ConsultationAiDraftNormalizer draftNormalizer;

    public ConsultationAiDraftResolutionResult resolveMappings(ConsultationSttSession session, ConsultationAiStructuredDraft draft) {
        if (draft == null) {
            return new ConsultationAiDraftResolutionResult(null, ConsultationAiResolutionResponse.empty());
        }

        List<ConsultationAiResolutionResponse.FieldResolution> fields = new ArrayList<>();

        if (draft.getAiHints() != null) {
            resolveContractHint(session, draft, fields);
            resolveProductHints(draft, fields);
            resolveDiseaseHints(draft, fields);
            draftNormalizer.normalizeAiHints(draft);
        }

        ensureContractSelectionCandidates(session, draft, fields);
        resolveStructuredFieldChoices(draft, fields);

        return new ConsultationAiDraftResolutionResult(
                draft,
                ConsultationAiResolutionResponse.builder()
                        .hasPendingResolution(fields.stream().anyMatch(field -> STATUS_NEEDS_USER_CONFIRMATION.equals(field.getStatus())))
                        .fields(List.copyOf(fields))
                        .build()
        );
    }

    private void resolveStructuredFieldChoices(ConsultationAiStructuredDraft draft, List<ConsultationAiResolutionResponse.FieldResolution> fields) {
        resolveCustomerInfoChoices(draft, fields);
        resolveNewDetailChoices(draft, fields);
        resolveClaimDetailChoices(draft, fields);
        resolveRenewalDetailChoices(draft, fields);
        resolveCancelDetailChoices(draft, fields);
    }

    private void resolveCustomerInfoChoices(ConsultationAiStructuredDraft draft, List<ConsultationAiResolutionResponse.FieldResolution> fields) {
        if (draft.getCustomerInfo() == null) {
            return;
        }
        ConsultationAiStructuredDraft.CustomerInfo customerInfo = draft.getCustomerInfo();
        customerInfo.setCustomerGender(resolveSingleChoiceField("customerInfo.customerGender", customerInfo.getCustomerGender(), GENDER_OPTIONS, false, fields));
        customerInfo.setCustomerMaritalStatus(resolveSingleChoiceField("customerInfo.customerMaritalStatus", customerInfo.getCustomerMaritalStatus(), MARITAL_STATUS_OPTIONS, false, fields));
        customerInfo.setCustomerIsSmoker(resolveBooleanLikeField("customerInfo.customerIsSmoker", customerInfo.getCustomerIsSmoker(), false, fields));
        customerInfo.setCustomerIsDrinker(resolveBooleanLikeField("customerInfo.customerIsDrinker", customerInfo.getCustomerIsDrinker(), false, fields));
    }

    private void resolveNewDetailChoices(ConsultationAiStructuredDraft draft, List<ConsultationAiResolutionResponse.FieldResolution> fields) {
        if (draft.getNewDetail() == null) {
            return;
        }
        ConsultationAiStructuredDraft.NewDetail newDetail = draft.getNewDetail();
        newDetail.setCoverageTypes(resolveMultiChoiceField("newDetail.coverageTypes", newDetail.getCoverageTypes(), COVERAGE_TYPE_OPTIONS, true, fields));
        newDetail.setHasExistingInsurance(resolveBooleanLikeField("newDetail.hasExistingInsurance", newDetail.getHasExistingInsurance(), false, fields));
    }

    private void resolveClaimDetailChoices(ConsultationAiStructuredDraft draft, List<ConsultationAiResolutionResponse.FieldResolution> fields) {
        if (draft.getClaimDetail() == null) {
            return;
        }
        ConsultationAiStructuredDraft.ClaimDetail claimDetail = draft.getClaimDetail();
        claimDetail.setClaimType(resolveSingleChoiceField("claimDetail.claimType", claimDetail.getClaimType(), CLAIM_TYPE_OPTIONS, true, fields));
        claimDetail.setReviewItems(resolveMultiChoiceField("claimDetail.reviewItems", claimDetail.getReviewItems(), CLAIM_REVIEW_ITEM_OPTIONS, true, fields));
        claimDetail.setHospitalizationStatus(resolveSingleChoiceField("claimDetail.hospitalizationStatus", claimDetail.getHospitalizationStatus(), YN_OPTIONS, true, fields));
        claimDetail.setSurgeryStatus(resolveSingleChoiceField("claimDetail.surgeryStatus", claimDetail.getSurgeryStatus(), YN_OPTIONS, true, fields));
    }

    private void resolveRenewalDetailChoices(ConsultationAiStructuredDraft draft, List<ConsultationAiResolutionResponse.FieldResolution> fields) {
        if (draft.getRenewalDetail() == null) {
            return;
        }
        ConsultationAiStructuredDraft.RenewalDetail renewalDetail = draft.getRenewalDetail();
        renewalDetail.setCoverageChangeType(resolveSingleChoiceField("renewalDetail.coverageChangeType", renewalDetail.getCoverageChangeType(), COVERAGE_CHANGE_TYPE_OPTIONS, true, fields));
        renewalDetail.setPremiumChangeReasonTypes(resolveMultiChoiceField("renewalDetail.premiumChangeReasonTypes", renewalDetail.getPremiumChangeReasonTypes(), PREMIUM_CHANGE_REASON_TYPE_OPTIONS, true, fields));
        renewalDetail.setInterestTypes(resolveMultiChoiceField("renewalDetail.interestTypes", renewalDetail.getInterestTypes(), COVERAGE_TYPE_OPTIONS, true, fields));
        renewalDetail.setCustomerReaction(resolveSingleChoiceField("renewalDetail.customerReaction", renewalDetail.getCustomerReaction(), CUSTOMER_REACTION_OPTIONS, true, fields));
        renewalDetail.setConsultationResult(resolveSingleChoiceField("renewalDetail.consultationResult", renewalDetail.getConsultationResult(), CONSULTATION_RESULT_OPTIONS, true, fields));
    }

    private void resolveCancelDetailChoices(ConsultationAiStructuredDraft draft, List<ConsultationAiResolutionResponse.FieldResolution> fields) {
        if (draft.getCancelDetail() == null) {
            return;
        }
        ConsultationAiStructuredDraft.CancelDetail cancelDetail = draft.getCancelDetail();
        cancelDetail.setReviewReasons(resolveMultiChoiceField("cancelDetail.reviewReasons", cancelDetail.getReviewReasons(), CANCELLATION_REVIEW_REASON_OPTIONS, true, fields));
        cancelDetail.setRetentionPlans(resolveMultiChoiceField("cancelDetail.retentionPlans", cancelDetail.getRetentionPlans(), RETENTION_PLAN_OPTIONS, true, fields));
        cancelDetail.setCustomerIntent(resolveSingleChoiceField("cancelDetail.customerIntent", cancelDetail.getCustomerIntent(), CUSTOMER_INTENT_OPTIONS, true, fields));
        cancelDetail.setRetentionPossibility(resolveSingleChoiceField("cancelDetail.retentionPossibility", cancelDetail.getRetentionPossibility(), RETENTION_POSSIBILITY_OPTIONS, true, fields));
        cancelDetail.setResult(resolveSingleChoiceField("cancelDetail.result", cancelDetail.getResult(), CONSULTATION_RESULT_OPTIONS, true, fields));
    }

    private String resolveSingleChoiceField(String fieldPath, String currentValue, List<ChoiceOption> options, boolean requiredSelection, List<ConsultationAiResolutionResponse.FieldResolution> fields) {
        String rawValue = trimToNull(currentValue);
        if (rawValue == null) {
            if (requiredSelection) {
                fields.add(buildFieldResolution(fieldPath, null, STATUS_NEEDS_USER_CONFIRMATION, "선택이 필요한 항목입니다.", options));
            }
            return null;
        }

        ChoiceOption matched = matchChoice(rawValue, options);
        if (matched != null) {
            if (!matched.code().equals(rawValue)) {
                fields.add(buildFieldResolution(fieldPath, rawValue, STATUS_AUTO_MAPPED, "입력값을 선택 코드로 정규화했습니다.", List.of(matched)));
            }
            return matched.code();
        }

        fields.add(buildFieldResolution(fieldPath, rawValue, requiredSelection ? STATUS_NEEDS_USER_CONFIRMATION : STATUS_NO_CANDIDATE, requiredSelection ? "사용자 확인이 필요합니다." : "선택 코드로 정규화하지 못했습니다.", options));
        return null;
    }

    private List<String> resolveMultiChoiceField(String fieldPath, List<String> currentValues, List<ChoiceOption> options, boolean requiredSelection, List<ConsultationAiResolutionResponse.FieldResolution> fields) {
        if (currentValues == null || currentValues.isEmpty()) {
            if (requiredSelection) {
                fields.add(buildFieldResolution(fieldPath, null, STATUS_NEEDS_USER_CONFIRMATION, "선택이 필요한 항목입니다.", options));
            }
            return currentValues;
        }

        LinkedHashSet<String> resolvedCodes = new LinkedHashSet<>();
        List<String> unresolvedValues = new ArrayList<>();
        for (String currentValue : currentValues) {
            String rawValue = trimToNull(currentValue);
            if (rawValue == null) {
                continue;
            }
            ChoiceOption matched = matchChoice(rawValue, options);
            if (matched != null) {
                resolvedCodes.add(matched.code());
                if (!matched.code().equals(rawValue)) {
                    fields.add(buildFieldResolution(fieldPath, rawValue, STATUS_AUTO_MAPPED, "입력값을 선택 코드로 정규화했습니다.", List.of(matched)));
                }
                continue;
            }
            unresolvedValues.add(rawValue);
        }

        if (!unresolvedValues.isEmpty()) {
            fields.add(buildFieldResolution(fieldPath, String.join(", ", unresolvedValues), requiredSelection ? STATUS_NEEDS_USER_CONFIRMATION : STATUS_NO_CANDIDATE, requiredSelection ? "사용자 확인이 필요한 값이 있습니다." : "일부 값을 선택 코드로 정규화하지 못했습니다.", options));
        }
        if (resolvedCodes.isEmpty() && requiredSelection) {
            fields.add(buildFieldResolution(fieldPath, null, STATUS_NEEDS_USER_CONFIRMATION, "선택이 필요한 항목입니다.", options));
        }
        return resolvedCodes.isEmpty() ? null : new ArrayList<>(resolvedCodes);
    }

    private Boolean resolveBooleanLikeField(String fieldPath, Boolean currentValue, boolean requiredSelection, List<ConsultationAiResolutionResponse.FieldResolution> fields) {
        if (currentValue != null) {
            return currentValue;
        }
        if (requiredSelection) {
            fields.add(buildFieldResolution(fieldPath, null, STATUS_NEEDS_USER_CONFIRMATION, "선택이 필요한 항목입니다.", BOOLEAN_OPTIONS));
        }
        return null;
    }

    private ConsultationAiResolutionResponse.FieldResolution buildFieldResolution(String fieldPath, String rawValue, String status, String message, List<ChoiceOption> options) {
        return ConsultationAiResolutionResponse.FieldResolution.builder()
                .fieldPath(fieldPath)
                .rawValue(rawValue)
                .status(status)
                .message(message)
                .candidates(options.stream().map(this::toChoiceCandidate).toList())
                .build();
    }

    private ChoiceOption matchChoice(String rawValue, List<ChoiceOption> options) {
        String normalizedRawValue = normalizeChoiceValue(rawValue);
        for (ChoiceOption option : options) {
            if (normalizeChoiceValue(option.code()).equals(normalizedRawValue)) {
                return option;
            }
        }
        for (ChoiceOption option : options) {
            if (normalizeChoiceValue(option.label()).equals(normalizedRawValue)) {
                return option;
            }
            if (option.synonyms().stream().map(this::normalizeChoiceValue).anyMatch(normalizedRawValue::equals)) {
                return option;
            }
        }
        for (ChoiceOption option : options) {
            if (normalizeChoiceValue(option.label()).contains(normalizedRawValue) || normalizedRawValue.contains(normalizeChoiceValue(option.label()))) {
                return option;
            }
            boolean synonymContains = option.synonyms().stream().map(this::normalizeChoiceValue).anyMatch(value -> value.contains(normalizedRawValue) || normalizedRawValue.contains(value));
            if (synonymContains) {
                return option;
            }
        }
        return null;
    }

    private String normalizeChoiceValue(String value) {
        String normalized = draftNormalizer.normalizeText(value);
        return normalized == null ? "" : normalized.replace(" ", "").toLowerCase(Locale.ROOT);
    }

    private ConsultationAiResolutionResponse.Candidate toChoiceCandidate(ChoiceOption option) {
        return ConsultationAiResolutionResponse.Candidate.builder()
                .id(option.code())
                .code(option.code())
                .label(option.label())
                .subLabel(option.synonyms().isEmpty() ? null : String.join(", ", option.synonyms()))
                .build();
    }

    private void resolveContractHint(ConsultationSttSession session, ConsultationAiStructuredDraft draft, List<ConsultationAiResolutionResponse.FieldResolution> fields) {
        ConsultationAiStructuredDraft.AiHints aiHints = draft.getAiHints();
        if (aiHints == null || trimToNull(aiHints.getTargetContractHint()) == null) {
            return;
        }
        if (draft.getContractId() != null || session == null || session.getCustomer() == null) {
            return;
        }

        String rawHint = aiHints.getTargetContractHint().trim();
        List<CustomerOwnedContractResponse> contracts = contractRepository.findOwnCustomerContracts(session.getCustomer().getId());
        List<CustomerOwnedContractResponse> candidates = contracts.stream().filter(contract -> matchesContractHint(contract, rawHint)).toList();
        if (candidates.size() == 1) {
            CustomerOwnedContractResponse candidate = candidates.get(0);
            draft.setContractId(candidate.getContractId());
            aiHints.setTargetContractHint(null);
            fields.add(ConsultationAiResolutionResponse.FieldResolution.builder().fieldPath("contractId").rawValue(rawHint).status(STATUS_AUTO_MAPPED).message("고객 보유 계약 기준으로 자동 매핑했습니다.").candidates(List.of(toContractCandidate(candidate))).build());
            return;
        }

        fields.add(ConsultationAiResolutionResponse.FieldResolution.builder().fieldPath("contractId").rawValue(rawHint).status(candidates.isEmpty() ? STATUS_NO_CANDIDATE : STATUS_NEEDS_USER_CONFIRMATION).message(candidates.isEmpty() ? "일치하는 계약 후보를 찾지 못했습니다." : "복수 계약 후보가 있어 확인이 필요합니다.").candidates(candidates.stream().map(this::toContractCandidate).toList()).build());
    }

    private void resolveProductHints(ConsultationAiStructuredDraft draft, List<ConsultationAiResolutionResponse.FieldResolution> fields) {
        if (draft.getAiHints() == null || draft.getAiHints().getMentionedProductNames() == null || draft.getNewDetail() == null) {
            return;
        }

        Set<String> productCodes = new LinkedHashSet<>();
        if (draft.getNewDetail().getProposedProductCodes() != null) {
            productCodes.addAll(draft.getNewDetail().getProposedProductCodes());
        }

        List<String> unresolvedNames = new ArrayList<>();
        for (String rawName : draft.getAiHints().getMentionedProductNames()) {
            if (trimToNull(rawName) == null) {
                continue;
            }
            ProductMatchResult result = findProductCandidates(rawName.trim());
            if (result.autoMappedCode() != null) {
                productCodes.add(result.autoMappedCode());
                fields.add(ConsultationAiResolutionResponse.FieldResolution.builder().fieldPath("newDetail.proposedProductCodes").rawValue(rawName.trim()).status(STATUS_AUTO_MAPPED).message("상품명 기준으로 자동 매핑했습니다.").candidates(result.candidates()).build());
                continue;
            }
            unresolvedNames.add(rawName.trim());
            fields.add(ConsultationAiResolutionResponse.FieldResolution.builder().fieldPath("newDetail.proposedProductCodes").rawValue(rawName.trim()).status(result.candidates().isEmpty() ? STATUS_NO_CANDIDATE : STATUS_NEEDS_USER_CONFIRMATION).message(result.candidates().isEmpty() ? "일치하는 상품 후보를 찾지 못했습니다." : "복수 상품 후보가 있어 확인이 필요합니다.").candidates(result.candidates()).build());
        }

        draft.getNewDetail().setProposedProductCodes(productCodes.isEmpty() ? null : new ArrayList<>(productCodes));
        draft.getAiHints().setMentionedProductNames(unresolvedNames.isEmpty() ? null : unresolvedNames);
    }

    private void ensureContractSelectionCandidates(ConsultationSttSession session, ConsultationAiStructuredDraft draft, List<ConsultationAiResolutionResponse.FieldResolution> fields) {
        if (draft == null || session == null || session.getCustomer() == null) {
            return;
        }
        if (draft.getConsultationType() == null || draft.getConsultationType() == ConsultationType.NEW_CONTRACT) {
            return;
        }
        if (draft.getContractId() != null || hasContractResolution(fields)) {
            return;
        }

        List<CustomerOwnedContractResponse> contracts = contractRepository.findOwnCustomerContracts(session.getCustomer().getId());
        if (contracts.isEmpty()) {
            return;
        }
        if (contracts.size() == 1) {
            CustomerOwnedContractResponse contract = contracts.get(0);
            draft.setContractId(contract.getContractId());
            fields.add(ConsultationAiResolutionResponse.FieldResolution.builder().fieldPath("contractId").rawValue(null).status(STATUS_AUTO_MAPPED).message("보유 계약이 1건이라 자동 연결했습니다.").candidates(List.of(toContractCandidate(contract))).build());
            return;
        }

        fields.add(ConsultationAiResolutionResponse.FieldResolution.builder().fieldPath("contractId").rawValue(null).status(STATUS_NEEDS_USER_CONFIRMATION).message("보유 계약 중 하나를 선택해 주세요.").candidates(contracts.stream().map(this::toContractCandidate).toList()).build());
    }

    private void resolveDiseaseHints(ConsultationAiStructuredDraft draft, List<ConsultationAiResolutionResponse.FieldResolution> fields) {
        if (draft.getAiHints() == null || draft.getAiHints().getMentionedDiseaseNames() == null || draft.getCustomerInfo() == null) {
            return;
        }

        Set<String> diseaseCodes = new LinkedHashSet<>();
        if (draft.getCustomerInfo().getUnderlyingDiseaseCodes() != null) {
            diseaseCodes.addAll(draft.getCustomerInfo().getUnderlyingDiseaseCodes());
        }

        List<String> unresolvedNames = new ArrayList<>();
        for (String rawName : draft.getAiHints().getMentionedDiseaseNames()) {
            if (trimToNull(rawName) == null) {
                continue;
            }
            DiseaseMatchResult result = findDiseaseCandidates(rawName.trim());
            if (result.autoMappedCode() != null) {
                diseaseCodes.add(result.autoMappedCode());
                fields.add(ConsultationAiResolutionResponse.FieldResolution.builder().fieldPath("customerInfo.underlyingDiseaseCodes").rawValue(rawName.trim()).status(STATUS_AUTO_MAPPED).message("질병명 기준으로 자동 매핑했습니다.").candidates(result.candidates()).build());
                continue;
            }
            unresolvedNames.add(rawName.trim());
            fields.add(ConsultationAiResolutionResponse.FieldResolution.builder().fieldPath("customerInfo.underlyingDiseaseCodes").rawValue(rawName.trim()).status(result.candidates().isEmpty() ? STATUS_NO_CANDIDATE : STATUS_NEEDS_USER_CONFIRMATION).message(result.candidates().isEmpty() ? "일치하는 질병 후보를 찾지 못했습니다." : "복수 질병 후보가 있어 확인이 필요합니다.").candidates(result.candidates()).build());
        }

        draft.getCustomerInfo().setUnderlyingDiseaseCodes(diseaseCodes.isEmpty() ? null : new ArrayList<>(diseaseCodes));
        draft.getAiHints().setMentionedDiseaseNames(unresolvedNames.isEmpty() ? null : unresolvedNames);
    }

    ProductMatchResult findProductCandidates(String rawName) {
        String resolvedProductCode = draftNormalizer.resolveProductCode(rawName);
        if (resolvedProductCode != null) {
            Optional<InsuranceProduct> codeMatch = insuranceProductRepository.findByInsuranceProductCodeAndDeletedAtIsNull(resolvedProductCode);
            if (codeMatch.isPresent()) {
                InsuranceProduct product = codeMatch.get();
                return new ProductMatchResult(product.getInsuranceProductCode(), List.of(toProductCandidate(product)));
            }
        }

        Optional<InsuranceProduct> exactMatch = insuranceProductRepository.findByInsuranceProductNameAndDeletedAtIsNull(rawName);
        if (exactMatch.isPresent()) {
            InsuranceProduct product = exactMatch.get();
            return new ProductMatchResult(product.getInsuranceProductCode(), List.of(toProductCandidate(product)));
        }

        List<InsuranceProduct> searchedProducts = searchProductCandidates(rawName);
        List<ScoredProductMatch> rankedMatches = searchedProducts.stream().map(product -> new ScoredProductMatch(product, scoreProductMatch(product, rawName))).filter(match -> match.score() > 0).sorted((left, right) -> Integer.compare(right.score(), left.score())).toList();
        if (rankedMatches.size() == 1 || shouldAutoMap(rankedMatches)) {
            InsuranceProduct product = rankedMatches.get(0).product();
            return new ProductMatchResult(product.getInsuranceProductCode(), List.of(toProductCandidate(product)));
        }
        return new ProductMatchResult(null, rankedMatches.stream().limit(5).map(ScoredProductMatch::product).map(this::toProductCandidate).toList());
    }

    private DiseaseMatchResult findDiseaseCandidates(String rawName) {
        Optional<DiseaseCode> exactMatch = diseaseCodeRepository.findByDiseaseNameAndDeletedAtIsNull(rawName);
        if (exactMatch.isPresent()) {
            DiseaseCode diseaseCode = exactMatch.get();
            return new DiseaseMatchResult(diseaseCode.getDiseaseCode(), List.of(toDiseaseCandidate(diseaseCode)));
        }
        List<DiseaseCode> containsMatches = diseaseCodeRepository.findTop10ByDiseaseNameContainingAndDeletedAtIsNullOrderByDiseaseNameAsc(rawName);
        List<DiseaseCode> filtered = containsMatches.stream().filter(diseaseCode -> isSimilar(draftNormalizer.normalizeText(diseaseCode.getDiseaseName()), draftNormalizer.normalizeText(rawName))).toList();
        if (filtered.size() == 1) {
            DiseaseCode diseaseCode = filtered.get(0);
            return new DiseaseMatchResult(diseaseCode.getDiseaseCode(), List.of(toDiseaseCandidate(diseaseCode)));
        }
        return new DiseaseMatchResult(null, filtered.stream().map(this::toDiseaseCandidate).toList());
    }

    private ConsultationAiResolutionResponse.Candidate toContractCandidate(CustomerOwnedContractResponse contract) {
        return ConsultationAiResolutionResponse.Candidate.builder().id(contract.getContractId().toString()).code(null).label(contract.getInsuranceProductName()).subLabel(buildContractSubLabel(contract)).build();
    }

    private String buildContractSubLabel(CustomerOwnedContractResponse contract) {
        List<String> parts = new ArrayList<>();
        if (trimToNull(contract.getInsuranceCompanyName()) != null) {
            parts.add(contract.getInsuranceCompanyName());
        }
        if (trimToNull(contract.getContractStatus()) != null) {
            parts.add(contract.getContractStatus());
        }
        if (contract.getMonthlyPremium() != null) {
            parts.add("보험료 " + contract.getMonthlyPremium().toPlainString() + "원");
        }
        return parts.isEmpty() ? null : String.join(" / ", parts);
    }

    private ConsultationAiResolutionResponse.Candidate toProductCandidate(InsuranceProduct product) {
        return ConsultationAiResolutionResponse.Candidate.builder().id(product.getId().toString()).code(product.getInsuranceProductCode()).label(product.getInsuranceProductName()).subLabel(product.getInsuranceCompany() != null ? product.getInsuranceCompany().getInsuranceCompanyName() : null).build();
    }

    private ConsultationAiResolutionResponse.Candidate toDiseaseCandidate(DiseaseCode diseaseCode) {
        return ConsultationAiResolutionResponse.Candidate.builder().id(diseaseCode.getId().toString()).code(diseaseCode.getDiseaseCode()).label(diseaseCode.getDiseaseName()).subLabel(diseaseCode.getDiseaseCategory()).build();
    }

    private boolean matchesContractHint(CustomerOwnedContractResponse contract, String hint) {
        String normalizedHint = draftNormalizer.normalizeText(hint);
        String productName = draftNormalizer.normalizeText(contract.getInsuranceProductName());
        String companyAndProduct = draftNormalizer.normalizeText((contract.getInsuranceCompanyName() == null ? "" : contract.getInsuranceCompanyName()) + " " + (contract.getInsuranceProductName() == null ? "" : contract.getInsuranceProductName()));
        return isSimilar(productName, normalizedHint) || isSimilar(companyAndProduct, normalizedHint);
    }

    private boolean isSimilar(String source, String target) {
        if (source == null || source.isBlank() || target == null || target.isBlank()) {
            return false;
        }
        return source.equals(target) || source.contains(target) || target.contains(source);
    }

    private boolean hasContractResolution(List<ConsultationAiResolutionResponse.FieldResolution> fields) {
        return fields.stream().anyMatch(field -> "contractId".equals(field.getFieldPath()));
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
        List<InsuranceProduct> matches = insuranceProductRepository.findTop10ByInsuranceProductNameContainingAndDeletedAtIsNullOrderByInsuranceProductNameAsc(keyword);
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
            if (PRODUCT_SEARCH_STOPWORDS.contains(normalizedToken.toLowerCase(Locale.ROOT))) {
                continue;
            }
            keywords.add(normalizedToken);
        }
        return new ArrayList<>(keywords);
    }

    private int scoreProductMatch(InsuranceProduct product, String rawName) {
        String normalizedProductName = draftNormalizer.normalizeText(product.getInsuranceProductName());
        String normalizedRawName = draftNormalizer.normalizeText(rawName);
        if (normalizedProductName == null || normalizedRawName == null) {
            return 0;
        }
        if (normalizedProductName.equals(normalizedRawName)) {
            return 1000;
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
            String normalizedKeyword = draftNormalizer.normalizeText(keyword);
            if (normalizedKeyword != null && normalizedProductName.contains(normalizedKeyword)) {
                score += 40;
            }
        }

        String inferredCode = draftNormalizer.resolveProductCode(rawName);
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
        String normalized = draftNormalizer.normalizeText(value);
        if (normalized == null) {
            return null;
        }
        return normalized.replace("insurance", "").replace("coverage", "").replace("rider", "").replace("plan", "").replace("product", "");
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
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[left.length()][right.length()];
    }

    private static ChoiceOption choice(String code, String label, String... synonyms) {
        return new ChoiceOption(code, label, List.of(synonyms));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    record ProductMatchResult(String autoMappedCode, List<ConsultationAiResolutionResponse.Candidate> candidates) {
    }

    private record DiseaseMatchResult(String autoMappedCode, List<ConsultationAiResolutionResponse.Candidate> candidates) {
    }

    private record ScoredProductMatch(InsuranceProduct product, int score) {
    }

    private record ChoiceOption(String code, String label, List<String> synonyms) {
    }
}
