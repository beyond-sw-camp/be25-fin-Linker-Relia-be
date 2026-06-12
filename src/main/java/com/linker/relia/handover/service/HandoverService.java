package com.linker.relia.handover.service;

import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.common.access.AccessScope;
import com.linker.relia.common.access.AccessScopeType;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.consultation.domain.ConsultationChannel;
import com.linker.relia.customer.domain.Customer;
import com.linker.relia.customer.repository.CustomerRepository;
import com.linker.relia.handover.domain.HandoverRecommendation;
import com.linker.relia.handover.domain.HandoverRequest;
import com.linker.relia.handover.domain.RequestStatus;
import com.linker.relia.handover.domain.RequestType;
import com.linker.relia.handover.dto.request.HandoverCreateRequest;
import com.linker.relia.handover.dto.response.HandoverCreateResponse;
import com.linker.relia.handover.dto.response.HandoverDetailResponse;
import com.linker.relia.handover.dto.response.HandoverListItemResponse;
import com.linker.relia.handover.dto.response.HandoverListResponse;
import com.linker.relia.handover.exception.HandoverErrorCode;
import com.linker.relia.handover.repository.HandoverDetailQueryRepository;
import com.linker.relia.handover.repository.HandoverRecommendationRepository;
import com.linker.relia.handover.repository.HandoverRequestRepository;
import com.linker.relia.security.principal.PrincipalDetails;
import com.linker.relia.user.domain.FpMonthlyInfo;
import com.linker.relia.user.domain.User;
import com.linker.relia.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class HandoverService {

    private final HandoverRequestRepository handoverRequestRepository;
    private final HandoverRecommendationRepository handoverRecommendationRepository;
    private final CustomerRepository customerRepository;
    private final HandoverDetailQueryRepository handoverDetailQueryRepository;
    private final RecommendationService recommendationService;

    public HandoverCreateResponse createHandover(PrincipalDetails principal, HandoverCreateRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new BusinessException(HandoverErrorCode.CUSTOMER_NOT_FOUND));

        validateHandoverCreateAccess(principal.getUser(), customer);

        boolean exists = handoverRequestRepository.existsByCustomerAndRequestStatusIn(
                customer,
                List.of(RequestStatus.MANAGER_PENDING, RequestStatus.RETRY)
        );
        if (exists) {
            throw new BusinessException(HandoverErrorCode.HANDOVER_REQUEST_ALREADY_EXISTS);
        }

        HandoverRequest handoverRequest = HandoverRequest.create(customer, request.requestType());
        handoverRequestRepository.save(handoverRequest);

        HandoverRecommendation recommendation = recommendationService.recommend(handoverRequest);
        handoverRecommendationRepository.save(recommendation);

        return HandoverCreateResponse.from(handoverRequest);
    }

    @Transactional(readOnly = true)
    public HandoverListResponse getHandoverList(PrincipalDetails principal,
                                                RequestStatus status,
                                                RequestType requestType,
                                                String customerName,
                                                Pageable pageable) {

        User user = principal.getUser();
        AccessScope accessScope = switch (user.getUserRole()) {
            case BRANCH_MANAGER -> {
                if (user.getOrganization() == null) {
                    throw new BusinessException(AuthErrorCode.INVALID_USER_STATE, "지점장 사용자에 조직 정보가 없습니다.");
                }
                yield new AccessScope(AccessScopeType.BRANCH, user.getId(), user.getOrganization().getId());
            }
            default -> new AccessScope(AccessScopeType.ALL, user.getId(), null);
        };

        Page<HandoverListItemResponse> page = handoverRequestRepository
                .searchHandovers(accessScope, status, requestType, customerName, pageable);

        return HandoverListResponse.of(page);
    }

    @Transactional(readOnly = true)
    public HandoverDetailResponse getHandoverDetail(PrincipalDetails principal, UUID handoverRequestId) {
        User user = principal.getUser();

        HandoverRequest handoverRequest = handoverRequestRepository.findById(handoverRequestId)
                .orElseThrow(() -> new BusinessException(HandoverErrorCode.HANDOVER_REQUEST_NOT_FOUND));

        HandoverRecommendation recommendation = handoverRecommendationRepository
                .findLatestByHandoverRequestAndApprovalStatus(handoverRequest, null, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(HandoverErrorCode.RECOMMENDATION_NOT_FOUND));

        validateHandoverDetailAccess(user, handoverRequest, recommendation);

        Customer customer = handoverRequest.getCustomer();
        UUID customerId = customer.getId();

        String contractSummary = handoverDetailQueryRepository.findContractSummary(customerId);
        BigDecimal monthlyPremium = handoverDetailQueryRepository.findMonthlyPremium(customerId);
        LocalDateTime lastConsultedAt = handoverDetailQueryRepository.findLastConsultedAt(customerId);
        ConsultationChannel mainChannel = handoverDetailQueryRepository
                .findMainChannel(customerId).orElse(null);
        Optional<Object[]> history = handoverDetailQueryRepository.findLatestHistory(customerId);

        String rejectedFpName = handoverDetailQueryRepository
                .findRejectedFpName(handoverRequestId).orElse(null);

        HandoverDetailResponse.CustomerInfo customerInfo = new HandoverDetailResponse.CustomerInfo(
                customer.getId(),
                customer.getCustomerName(),
                customer.getCustomerGrade(),
                handoverRequest.getCurrentFp() != null ? handoverRequest.getCurrentFp().getUserName() : null,
                contractSummary,
                monthlyPremium,
                lastConsultedAt,
                mainChannel
        );

        HandoverDetailResponse.HandoverHistoryInfo historyInfo = history
                .map(h -> new HandoverDetailResponse.HandoverHistoryInfo((String) h[0], (LocalDateTime) h[1]))
                .orElse(new HandoverDetailResponse.HandoverHistoryInfo(null, null));

        Optional<FpMonthlyInfo> fpInfo = handoverDetailQueryRepository
                .findLatestFpMonthlyInfo(recommendation.getRecommendedFp().getEmpCode());

        HandoverDetailResponse.RecommendationInfo recommendationInfo = new HandoverDetailResponse.RecommendationInfo(
                recommendation.getId(),
                recommendation.getRecommendedFpName(),
                fpInfo.map(info -> List.of(info.getSpecialtyCategory().split(","))).orElse(List.of()),
                fpInfo.map(FpMonthlyInfo::getRetentionRate).orElse(null),
                fpInfo.map(FpMonthlyInfo::getPreferredCustomerAge).orElse(null),
                fpInfo.map(FpMonthlyInfo::getConsultationChannel).orElse(null),
                recommendation.getRecommendationReason(),
                recommendation.getApprovalStatus(),
                rejectedFpName
        );

        boolean canApprove = user.getUserRole() == UserRole.BRANCH_MANAGER;

        return new HandoverDetailResponse(
                handoverRequest.getId(),
                handoverRequest.getRequestType(),
                handoverRequest.getRequestStatus(),
                handoverRequest.getCreatedAt(),
                customerInfo,
                historyInfo,
                recommendationInfo,
                canApprove
        );
    }

    private void validateHandoverDetailAccess(
            User user,
            HandoverRequest handoverRequest,
            HandoverRecommendation recommendation
    ) {
        switch (user.getUserRole()) {
            case BRANCH_MANAGER -> validateBranchManagerAccess(user, handoverRequest);
            case FP -> validateFpAccess(user, handoverRequest, recommendation);
            default -> {
            }
        }
    }

    private void validateHandoverCreateAccess(User user, Customer customer) {
        if (user.getUserRole() != UserRole.BRANCH_MANAGER) {
            return;
        }

        if (user.getOrganization() == null) {
            throw new BusinessException(AuthErrorCode.INVALID_USER_STATE, "지점장 사용자에 조직 정보가 없습니다.");
        }

        User customerFp = customer.getCustomerFp();
        if (customerFp == null || customerFp.getOrganization() == null) {
            throw new BusinessException(AuthErrorCode.INVALID_USER_STATE, "인수인계 고객의 담당 설계사 또는 조직 정보가 없습니다.");
        }

        if (!customerFp.getOrganization().getId().equals(user.getOrganization().getId())) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN, "소속 지점 고객에 대해서만 인수인계 요청을 생성할 수 있습니다.");
        }
    }

    private void validateBranchManagerAccess(User user, HandoverRequest handoverRequest) {
        if (user.getOrganization() == null) {
            throw new BusinessException(AuthErrorCode.INVALID_USER_STATE, "지점장 사용자에 조직 정보가 없습니다.");
        }

        Customer customer = handoverRequest.getCustomer();
        User customerFp = customer.getCustomerFp();
        if (customerFp == null || customerFp.getOrganization() == null) {
            throw new BusinessException(AuthErrorCode.INVALID_USER_STATE, "인수인계 고객의 담당 설계사 또는 조직 정보가 없습니다.");
        }

        UUID requestOrgId = customerFp.getOrganization().getId();
        if (!requestOrgId.equals(user.getOrganization().getId())) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
        }
    }

    private void validateFpAccess(
            User user,
            HandoverRequest handoverRequest,
            HandoverRecommendation recommendation
    ) {
        boolean isCurrentFp = handoverRequest.getCurrentFp() != null
                && handoverRequest.getCurrentFp().getId().equals(user.getId());
        boolean isRecommendedFp = recommendation.getRecommendedFp() != null
                && recommendation.getRecommendedFp().getId().equals(user.getId());

        if (!isCurrentFp && !isRecommendedFp) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
        }
    }
}
