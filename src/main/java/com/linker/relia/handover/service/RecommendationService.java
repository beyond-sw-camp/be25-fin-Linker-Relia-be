package com.linker.relia.handover.service;

import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.customer.domain.Customer;
import com.linker.relia.handover.domain.ApprovalStatus;
import com.linker.relia.handover.domain.HandoverRecommendation;
import com.linker.relia.handover.domain.HandoverRequest;
import com.linker.relia.handover.exception.HandoverErrorCode;
import com.linker.relia.handover.repository.HandoverRecommendationQueryRepository;
import com.linker.relia.handover.repository.HandoverRecommendationRepository;
import com.linker.relia.user.domain.FpMonthlyInfo;
import com.linker.relia.user.domain.User;
import com.linker.relia.user.domain.UserRole;
import com.linker.relia.user.domain.UserStatus;
import com.linker.relia.user.repository.FpMonthlyInfoRepository;
import com.linker.relia.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecommendationService {

    private static final BigDecimal DEFAULT_RETENTION_RATE = BigDecimal.ZERO;

    private final UserRepository userRepository;
    private final FpMonthlyInfoRepository fpMonthlyInfoRepository;
    private final HandoverRecommendationRepository handoverRecommendationRepository;
    private final HandoverRecommendationQueryRepository handoverRecommendationQueryRepository;

    public HandoverRecommendation recommend(HandoverRequest handoverRequest) {
        Customer customer = handoverRequest.getCustomer();

        // 1. 고객 프로파일 수집
        CustomerProfile profile = buildCustomerProfile(customer);

        // 2. 같은 지점 설계사 목록 User-> Organization
        String organizationCode = customer.getCustomerFp()
                .getOrganization()
                .getOrganizationCode();

        Set<String> excludedEmpCodes = new HashSet<>(
                handoverRecommendationQueryRepository.findCustomerHistoryFpEmpCodes(customer.getId())
        ); // 고객 설계사 변경 이력에 있는 설계사 제외
        excludedEmpCodes.addAll(
                handoverRecommendationQueryRepository.findRecommendedFpEmpCodes(handoverRequest.getId())
        ); // 추천 거절된 설계사 제외
        excludedEmpCodes.add(customer.getCustomerFp().getEmpCode());

        Map<String, FpMonthlyInfo> latestMonthlyInfoByEmpCode = fpMonthlyInfoRepository
                .findLatestByOrganizationCode(organizationCode)
                .stream()
                .collect(toMap(
                        FpMonthlyInfo::getEmpCode,
                        Function.identity(),
                        (current, ignored) -> current
                ));

        List<FpRecommendationCandidate> candidates = userRepository
                .findByOrganizationOrganizationCodeAndUserRoleAndUserStatusAndDeletedAtIsNullOrderByUserNameAsc(
                        organizationCode,
                        UserRole.FP,
                        UserStatus.ACTIVE
                )
                .stream()
                .filter(fp -> fp.getEmpCode() != null)
                .filter(fp -> !excludedEmpCodes.contains(fp.getEmpCode()))
                .map(fp -> new FpRecommendationCandidate(fp, latestMonthlyInfoByEmpCode.get(fp.getEmpCode())))
                .toList();

        // 3. 지점 평균 계약수 계산 (계약수 페널티 기준)
        double avgContractCount = candidates.stream()
                .mapToInt(this::getCurrentContractCount)
                .average()
                .orElse(0);

        // 4. 현재 pending 건수 맵 (추천 대기 페널티 기준)
        Map<String, Long> pendingCountMap = handoverRecommendationRepository
                .findLatestByHandoverRequestAndApprovalStatus(
                        handoverRequest,
                        ApprovalStatus.PENDING,
                        PageRequest.of(0, 1)
                )
                .stream()
                .collect(groupingBy(
                        recommendation -> recommendation.getRecommendedFp().getEmpCode(),
                        counting()
                ));

        // 5. 스코어링 후 최고점 설계사 선정
        FpRecommendationCandidate best = candidates.stream()
                .max(Comparator.comparingInt(candidate ->
                        calculateScore(candidate, profile, avgContractCount, pendingCountMap)))
                .orElseThrow(() -> new BusinessException(HandoverErrorCode.NO_AVAILABLE_FP));

        // 6. 추천 사유 생성
        String reason = buildReason(best, profile);
        return HandoverRecommendation.create(handoverRequest, best.user(), reason);
    }

    private int calculateScore(FpRecommendationCandidate candidate,
                               CustomerProfile profile,
                               double avgContractCount,
                               Map<String, Long> pendingCountMap) {
        FpMonthlyInfo fp = candidate.monthlyInfo();
        int score = 0;

        // 보종 매칭 +40
        if (fp != null) {
            if (isCategoryMatched(profile, fp)) {
                score += 40;
            }

            // 유지율 +30 (100%면 30점 만점)
            score += getRetentionRate(fp).multiply(BigDecimal.valueOf(0.3)).intValue();

            // 연령대 매칭 +20
            if (fp.getPreferredCustomerAge() != null
                    && fp.getPreferredCustomerAge() == profile.ageGroup()) {
                score += 20;
            }

            // 상담 채널 매칭 +10
            if (fp.getConsultationChannel() != null
                    && fp.getConsultationChannel().equals(profile.mainChannel())) {
                score += 10;
            }
        }

        return applyLoadPenalty(
                score,
                candidate.user().getEmpCode(),
                getCurrentContractCount(candidate),
                avgContractCount,
                pendingCountMap
        );
    }

    // 계약수 페널티 -15 (지점 평균 초과 시)
    private int applyLoadPenalty(int score,
                                 String empCode,
                                 int currentContractCount,
                                 double avgContractCount,
                                 Map<String, Long> pendingCountMap) {
        if (currentContractCount > avgContractCount) {
            score -= 15;
        }

        // pending 페널티 -15 (2건 이상 추천된 설계사)
        long pendingCount = pendingCountMap.getOrDefault(empCode, 0L);
        if (pendingCount >= 2) {
            score -= 15;
        }

        return score;
    }

    private CustomerProfile buildCustomerProfile(Customer customer) {
        // 고객 보종 목록 조회
        List<String> categoryList = handoverRecommendationQueryRepository
                .findCustomerCategories(customer.getId());

        // 고객 주 상담 채널 조회
        String mainChannel = handoverRecommendationQueryRepository
                .findMainChannel(customer.getId());

        // 연령대 계산 (10단위)
        int ageGroup = 0;
        if (customer.getCustomerBirthDate() != null) {
            int age = Period.between(customer.getCustomerBirthDate(), LocalDate.now()).getYears();
            ageGroup = Math.max(age, 0) / 10 * 10;
        }

        return new CustomerProfile(categoryList, mainChannel, ageGroup);
    }

    private String buildReason(FpRecommendationCandidate candidate, CustomerProfile profile) {
        FpMonthlyInfo fp = candidate.monthlyInfo();
        if (fp == null) {
            return candidate.user().getUserName()
                    + " 설계사는 현재 배정 가능한 활성 설계사로, 실적 기준 데이터가 없어 기본 추천 후보로 선정되었습니다.";
        }

        List<String> reasons = new ArrayList<>();

        if (isCategoryMatched(profile, fp)) {
            reasons.add(fp.getSpecialtyCategory() + " 보종 전문");
        }
        if (getRetentionRate(fp).compareTo(BigDecimal.valueOf(90)) >= 0) {
            reasons.add("유지율 " + getRetentionRate(fp) + "%");
        }
        if (fp.getConsultationChannel() != null && fp.getConsultationChannel().equals(profile.mainChannel())) {
            reasons.add(fp.getConsultationChannel() + " 상담 선호 고객에 적합");
        }

        return reasons.isEmpty()
                ? candidate.user().getUserName() + " 설계사는 현재 배정 가능한 활성 설계사입니다."
                : String.join(", ", reasons);
    }

    private boolean isCategoryMatched(CustomerProfile profile, FpMonthlyInfo fp) {
        if (fp.getSpecialtyCategory() == null) {
            return false;
        }

        return List.of(fp.getSpecialtyCategory().split(","))
                .stream()
                .map(String::trim)
                .anyMatch(profile.categoryList()::contains);
    }

    private int getCurrentContractCount(FpRecommendationCandidate candidate) {
        return candidate.monthlyInfo() == null
                ? 0
                : candidate.monthlyInfo().getCurrentContractCount();
    }

    private BigDecimal getRetentionRate(FpMonthlyInfo fp) {
        return fp == null || fp.getRetentionRate() == null
                ? DEFAULT_RETENTION_RATE
                : fp.getRetentionRate();
    }

    private record CustomerProfile(
            List<String> categoryList,
            String mainChannel,
            int ageGroup
    ) {}

    private record FpRecommendationCandidate(
            User user,
            FpMonthlyInfo monthlyInfo
    ) {}
}
