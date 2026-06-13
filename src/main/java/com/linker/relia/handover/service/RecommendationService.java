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

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecommendationService {

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
                .getOrganization().getOrganizationCode();
        Set<String> excludedEmpCodes = new HashSet<>(
                handoverRecommendationQueryRepository.findCustomerHistoryFpEmpCodes(customer.getId())
        ); // 고객 설계사 변경 이력에 있는 설계사 제외
        excludedEmpCodes.addAll(
                handoverRecommendationQueryRepository.findRecommendedFpEmpCodes(handoverRequest.getId())
        ); // 추천 거절된 설계사 제외
        excludedEmpCodes.add(customer.getCustomerFp().getEmpCode());

        List<FpMonthlyInfo> candidates = fpMonthlyInfoRepository
                .findLatestByOrganizationCode(organizationCode)
                .stream()
                .filter(fp -> !excludedEmpCodes.contains(fp.getEmpCode()))
                .toList();

        // 3. 지점 평균 계약수 계산 (계약수 페널티 기준)
        double avgContractCount = candidates.stream()
                .mapToInt(FpMonthlyInfo::getCurrentContractCount)
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
                        r -> r.getRecommendedFp().getEmpCode(),
                        counting()
                ));

        // 5. 스코어링 후 최고점 설계사 선정
        FpMonthlyInfo best = candidates.stream()
                .max(Comparator.comparingInt(fp ->
                        calculateScore(fp, profile, avgContractCount, pendingCountMap)))
                .orElseThrow(() -> new BusinessException(HandoverErrorCode.NO_AVAILABLE_FP));

        // 6. 추천 사유 생성
        String reason = buildReason(best, profile);

        // 7. User 엔티티 조회 후 HandoverRecommendation 생성
        User recommendedUser = userRepository.findByEmpCode(best.getEmpCode())
                .orElseThrow(() -> new BusinessException(HandoverErrorCode.FP_NOT_FOUND));
        return HandoverRecommendation.create(handoverRequest, recommendedUser, reason);
    }

    private int calculateScore(FpMonthlyInfo fp, CustomerProfile profile,
                               double avgContractCount, Map<String, Long> pendingCountMap) {
        int score = 0;

        // 보종 매칭 +40
        if (profile.categoryList().contains(fp.getSpecialtyCategory())) {
            score += 40;
        }

        // 유지율 +30 (100%면 30점 만점)
        score += fp.getRetentionRate().multiply(BigDecimal.valueOf(0.3)).intValue(); // BigDecimal: 정확한 소수점을 다루기 위해

        // 연령대 매칭 +20
        if (fp.getPreferredCustomerAge() != null
                && fp.getPreferredCustomerAge() == profile.ageGroup()) {
            score += 20;
        }

        // 상담 채널 매칭 +10
        if (fp.getConsultationChannel().equals(profile.mainChannel())) {
            score += 10;
        }

        // 계약수 페널티 -15 (지점 평균 초과 시)
        if (fp.getCurrentContractCount() > avgContractCount) {
            score -= 15;
        }

        // pending 페널티 -15 (2건 이상 추천된 설계사)
        long pendingCount = pendingCountMap.getOrDefault(fp.getEmpCode(), 0L);
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
        int age = Period.between(customer.getCustomerBirthDate(), LocalDate.now()).getYears();
        int ageGroup = (age / 10) * 10;

        return new CustomerProfile(categoryList, mainChannel, ageGroup);
    }

    private String buildReason(FpMonthlyInfo fp, CustomerProfile profile) {
        List<String> reasons = new ArrayList<>();

        if (profile.categoryList().contains(fp.getSpecialtyCategory())) {
            reasons.add(fp.getSpecialtyCategory() + " 보종 전문");
        }
        if (fp.getRetentionRate().compareTo(BigDecimal.valueOf(90)) >= 0) {
            reasons.add("유지율 " + fp.getRetentionRate() + "%");
        }
        if (fp.getConsultationChannel().equals(profile.mainChannel())) {
            reasons.add(fp.getConsultationChannel() + " 상담 선호 고객에 적합");
        }

        return String.join(", ", reasons);
    }

    private record CustomerProfile(
            List<String> categoryList,
            String mainChannel,
            int ageGroup
    ) {}

}
