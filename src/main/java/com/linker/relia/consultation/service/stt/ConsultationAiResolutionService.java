package com.linker.relia.consultation.service.stt;

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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ConsultationAiResolutionService {
    private static final Pattern PRODUCT_KEYWORD_SPLIT_PATTERN = Pattern.compile("[\\s,/()\\[\\]{}]+");
    private static final Set<String> PRODUCT_SEARCH_STOPWORDS = Set.of(
            "보험", "보장", "특약", "상품", "플랜", "추천", "가입", "설계", "상담", "고객", "본"
    );

    private final InsuranceProductRepository insuranceProductRepository;
    private final ContractRepository contractRepository;
    private final DiseaseCodeRepository diseaseCodeRepository;
    private final ConsultationAiDraftNormalizer draftNormalizer;

    public ConsultationAiDraftResolutionResult resolveMappings(
            ConsultationSttSession session,
            ConsultationAiStructuredDraft draft
    ) {
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

        return new ConsultationAiDraftResolutionResult(
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

    private void ensureContractSelectionCandidates(
            ConsultationSttSession session,
            ConsultationAiStructuredDraft draft,
            List<ConsultationAiResolutionResponse.FieldResolution> fields
    ) {
        if (draft == null || session == null || session.getCustomer() == null) {
            return;
        }
        if (draft.getConsultationType() == null || draft.getConsultationType().name().equals("NEW_CONTRACT")) {
            return;
        }
        if (draft.getContractId() != null || hasContractResolution(fields)) {
            return;
        }

        List<CustomerOwnedContractResponse> contracts =
                contractRepository.findOwnCustomerContracts(session.getCustomer().getId());
        if (contracts.isEmpty()) {
            return;
        }

        if (contracts.size() == 1) {
            CustomerOwnedContractResponse contract = contracts.get(0);
            draft.setContractId(contract.getContractId());
            fields.add(ConsultationAiResolutionResponse.FieldResolution.builder()
                    .fieldPath("contractId")
                    .rawValue(null)
                    .status("AUTO_MAPPED")
                    .message("선택된 고객의 보유 계약이 1건이라 자동으로 연결했습니다.")
                    .candidates(List.of(toContractCandidate(contract)))
                    .build());
            return;
        }

        fields.add(ConsultationAiResolutionResponse.FieldResolution.builder()
                .fieldPath("contractId")
                .rawValue(null)
                .status("NEEDS_USER_CONFIRMATION")
                .message("선택된 고객의 보유 계약 목록에서 계약을 선택해 주세요.")
                .candidates(contracts.stream().map(this::toContractCandidate).toList())
                .build());
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

    ProductMatchResult findProductCandidates(String rawName) {
        String resolvedProductCode = draftNormalizer.resolveProductCode(rawName);
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
                .filter(diseaseCode -> isSimilar(draftNormalizer.normalizeText(diseaseCode.getDiseaseName()), draftNormalizer.normalizeText(rawName)))
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
                .subLabel(buildContractSubLabel(contract))
                .build();
    }

    private String buildContractSubLabel(CustomerOwnedContractResponse contract) {
        List<String> parts = new ArrayList<>();
        if (contract.getInsuranceCompanyName() != null && !contract.getInsuranceCompanyName().isBlank()) {
            parts.add(contract.getInsuranceCompanyName());
        }
        if (contract.getContractStatus() != null && !contract.getContractStatus().isBlank()) {
            parts.add(contract.getContractStatus());
        }
        if (contract.getMonthlyPremium() != null) {
            parts.add("보험료 " + contract.getMonthlyPremium().toPlainString() + "원");
        }
        return parts.isEmpty() ? null : String.join(" / ", parts);
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
        String normalizedHint = draftNormalizer.normalizeText(hint);
        String productName = draftNormalizer.normalizeText(contract.getInsuranceProductName());
        String companyAndProduct = draftNormalizer.normalizeText(contract.getInsuranceCompanyName() + " " + contract.getInsuranceProductName());

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
        String normalizedProductName = draftNormalizer.normalizeText(product.getInsuranceProductName());
        String normalizedRawName = draftNormalizer.normalizeText(rawName);
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

    record ProductMatchResult(
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
