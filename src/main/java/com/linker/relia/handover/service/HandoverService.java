package com.linker.relia.handover.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.common.access.AccessScope;
import com.linker.relia.common.access.AccessScopeType;
import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.common.exception.CommonErrorCode;
import com.linker.relia.consultation.domain.ConsultationChannel;
import com.linker.relia.customer.domain.Customer;
import com.linker.relia.customer.domain.CustomerFpHistory;
import com.linker.relia.customer.repository.CustomerFpHistoryRepository;
import com.linker.relia.customer.repository.CustomerRepository;
import com.linker.relia.handover.domain.ApprovalStatus;
import com.linker.relia.handover.domain.HandoverRecommendation;
import com.linker.relia.handover.domain.HandoverRequest;
import com.linker.relia.handover.domain.RequestStatus;
import com.linker.relia.handover.domain.RequestType;
import com.linker.relia.handover.dto.request.HandoverApprovalRequest;
import com.linker.relia.handover.dto.request.HandoverAssignRequest;
import com.linker.relia.handover.dto.request.HandoverCreateRequest;
import com.linker.relia.handover.dto.response.HandoverAssignableFpResponse;
import com.linker.relia.handover.dto.response.HandoverBranchSummaryResponse;
import com.linker.relia.handover.dto.response.HandoverCreateResponse;
import com.linker.relia.handover.dto.response.HandoverDetailResponse;
import com.linker.relia.handover.dto.response.HandoverListItemResponse;
import com.linker.relia.handover.dto.response.HandoverMonthlyTrendResponse;
import com.linker.relia.handover.dto.response.HandoverReceivedItemResponse;
import com.linker.relia.handover.dto.response.HandoverReceivedSummaryResponse;
import com.linker.relia.handover.dto.response.HandoverSummaryResponse;
import com.linker.relia.handover.exception.HandoverErrorCode;
import com.linker.relia.handover.event.HandoverSmsEvent;
import com.linker.relia.handover.repository.HandoverDetailQueryRepository;
import com.linker.relia.handover.repository.HandoverReceivedQueryRepository;
import com.linker.relia.handover.repository.HandoverRecommendationRepository;
import com.linker.relia.handover.repository.HandoverRequestRepository;
import com.linker.relia.notification.NotificationEvent;
import com.linker.relia.notification.NotificationPublisher;
import com.linker.relia.notification.domain.NotificationType;
import com.linker.relia.security.principal.PrincipalDetails;
import com.linker.relia.user.domain.FpMonthlyInfo;
import com.linker.relia.user.domain.User;
import com.linker.relia.user.domain.UserRole;
import com.linker.relia.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class HandoverService {

    private final HandoverRequestRepository handoverRequestRepository;
    private final HandoverRecommendationRepository handoverRecommendationRepository;
    private final CustomerRepository customerRepository;
    private final CustomerFpHistoryRepository customerFpHistoryRepository;
    private final HandoverDetailQueryRepository handoverDetailQueryRepository;
    private final RecommendationService recommendationService;
    private final HandoverReceivedQueryRepository handoverReceivedQueryRepository;
    private final UserRepository userRepository;
    private final NotificationPublisher notificationPublisher;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    // 인수인계 요청 생성
    public HandoverCreateResponse createHandover(PrincipalDetails principal, HandoverCreateRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new BusinessException(HandoverErrorCode.CUSTOMER_NOT_FOUND));

        validateHandoverCreateAccess(principal.getUser(), customer);

        if (existsPendingHandover(customer)) {
            throw new BusinessException(HandoverErrorCode.HANDOVER_REQUEST_ALREADY_EXISTS);
        }

        HandoverRequest handoverRequest = createHandoverRequest(customer, request.requestType(), true);

        return HandoverCreateResponse.from(handoverRequest);
    }

    // 해촉은 고객 리스트를 돌지만, 고객 1명당 인수인계 요청은 이 단건 로직으로 생성한다.
    public Optional<HandoverRequest> createResignationHandoverIfAbsent(Customer customer) {
        // 기존 해촉 정책 유지: 이미 대기 중인 요청이 있는 고객은 전체 해촉을 실패시키지 않고 스킵한다.
        if (existsPendingHandover(customer)) {
            return Optional.empty();
        }

        return Optional.of(createHandoverRequest(customer, RequestType.RESIGNATION, true));
    }

    private HandoverRequest createHandoverRequest(Customer customer, RequestType requestType, boolean publishNotification) {
        HandoverRequest handoverRequest = HandoverRequest.create(customer, requestType);
        handoverRequestRepository.save(handoverRequest);

        // 추천 계산과 추천 사유 생성까지 포함한 실제 인수인계 생성 공통 로직이다.
        HandoverRecommendation recommendation = recommendationService.recommend(handoverRequest);
        handoverRecommendationRepository.save(recommendation);

        if (publishNotification) {
            User branchManager = findBranchManager(recommendation.getRecommendedFp());

            // 단건 API 생성은 기존처럼 지점장에게 결재 요청 알림을 보낸다.
            notificationPublisher.publish(NotificationEvent.builder()
                    .receiverUserId(branchManager.getId())
                    .type(NotificationType.HANDOVER_REQUEST)
                    .message(customer.getCustomerName() + " 고객 건의 인수인계 결재 요청이 있습니다.")
                    .referenceId(handoverRequest.getId())
                    .build());
        }

        return handoverRequest;
    }

    private boolean existsPendingHandover(Customer customer) {
        return handoverRequestRepository.existsByCustomerAndRequestStatusIn(
                customer,
                List.of(RequestStatus.MANAGER_PENDING)
        );
    }

    // 인수인계 요청 조회
    @Transactional(readOnly = true)
    public PageResponse<HandoverListItemResponse> getHandoverList(PrincipalDetails principal,
                                                                   RequestStatus status,
                                                                   RequestType requestType,
                                                                   String customerName, String organizationCode,
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

        String normalizedOrganizationCode = normalizeNullable(organizationCode);
        Page<HandoverListItemResponse> page = handoverRequestRepository
                .searchHandovers(accessScope, status, requestType, customerName, normalizedOrganizationCode, pageable);

        return PageResponse.from(page);
    }


    // 인수인계 상세 조회
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
                calculateAge(customer.getCustomerBirthDate()),
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
                .findLatestFpMonthlyInfo(
                        recommendation.getRecommendedFp().getEmpCode()
                );

        HandoverDetailResponse.RecommendationInfo recommendationInfo = new HandoverDetailResponse.RecommendationInfo(
                recommendation.getId(),
                recommendation.getRecommendedFpName(),
                fpInfo.map(info -> List.of(info.getSpecialtyCategory().split(","))).orElse(List.of()),
                fpInfo.map(FpMonthlyInfo::getRetentionRate).orElse(null),
                fpInfo.map(FpMonthlyInfo::getPreferredCustomerAge).orElse(null),
                fpInfo.map(FpMonthlyInfo::getConsultationChannel).orElse(null),
                recommendation.getRecommendationReason(),
                parseMatchingReasons(recommendation.getMatchingReasonsJson()),
                recommendation.getApprovalStatus(),
                rejectedFpName
        );

        boolean canApprove = user.getUserRole() == UserRole.BRANCH_MANAGER
                && isApprovalProcessable(handoverRequest);

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


    // 지점장 인수인계 결재
    @Transactional
    public void processApproval(PrincipalDetails principal,
                                UUID handoverRequestId,
                                HandoverApprovalRequest request) {

        if (request == null || request.approvalStatus() == null) {
            throw new BusinessException(HandoverErrorCode.INVALID_APPROVAL_REQUEST);
        }

        User user = principal.getUser();

        // 1. 요청 조회
        HandoverRequest handoverRequest = handoverRequestRepository.findById(handoverRequestId)
                .orElseThrow(() -> new BusinessException(HandoverErrorCode.HANDOVER_REQUEST_NOT_FOUND));

        // 2. 권한 체크 (BRANCH_MANAGER만 결재 가능)
        validateApprovalProcessTarget(handoverRequest);

        if (user.getUserRole() != UserRole.BRANCH_MANAGER) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
        }
        if (user.getOrganization() == null) {
            throw new BusinessException(AuthErrorCode.INVALID_USER_STATE);
        }

        Customer customer = handoverRequest.getCustomer();
        User customerFp = customer.getCustomerFp();
        if (customerFp == null || customerFp.getOrganization() == null) {
            throw new BusinessException(HandoverErrorCode.INVALID_HANDOVER_APPROVAL_TARGET);
        }

        if (!customerFp.getOrganization().getId().equals(user.getOrganization().getId())) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
        }

        // 3. 현재 PENDING 추천 조회
        HandoverRecommendation recommendation = handoverRecommendationRepository
                .findLatestByHandoverRequestAndApprovalStatus(
                        handoverRequest,
                        ApprovalStatus.PENDING,
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(HandoverErrorCode.RECOMMENDATION_NOT_FOUND));

        if (request.approvalStatus() == ApprovalStatus.APPROVED) {
            // ── 승인 처리 ──
            // 4-1. 추천 승인
            recommendation.approve(user.getId());

            User newFp = recommendation.getRecommendedFp();

            // 4-2. 담당 설계사 변경
            customer.changeCustomerFp(newFp);

            // 4-3. 이력 저장
            int nextSequence = customerFpHistoryRepository.findMaxCustomerFpSequence(customer.getId()) + 1;
            CustomerFpHistory history = CustomerFpHistory.create(
                    customer,
                    handoverRequest.getId(),
                    handoverRequest.getCurrentFp(),
                    recommendation.getRecommendedFp(),
                    "인수인계 승인"
            );
            history.applyChangeMetadata(user, nextSequence);
            customerFpHistoryRepository.save(history);

            // 4-4. 요청 완료
            handoverRequest.complete();

            // SSE 알림
            notificationPublisher.publish(NotificationEvent.builder()
                    .receiverUserId(newFp.getId())
                    .type(NotificationType.HANDOVER_RECEIVED)
                    .message(customer.getCustomerName() + " 고객이 담당 고객으로 배정되었습니다.")
                    .referenceId(handoverRequest.getId())
                    .build());

            // SMS 발송
            eventPublisher.publishEvent(new HandoverSmsEvent(
                    customer.getCustomerPhone(),
                    newFp.getUserName()
            ));

        } else {
            // ── 반려 처리 ──
            // 4-1. 추천 반려
            recommendation.reject(user.getId());
            if (request.rejectionReason() != null) {
                recommendation.setRejectionReason(request.rejectionReason());
            }

            // 4-2. 새 추천 자동 실행
            HandoverRecommendation newRecommendation = recommendationService.recommend(handoverRequest);
            handoverRecommendationRepository.save(newRecommendation);
        }
    }

    // 설계사가 받은 인수인계 목록
    @Transactional(readOnly = true)
    public PageResponse<HandoverReceivedItemResponse> getReceivedList(PrincipalDetails principal, Pageable pageable) {

        if (principal == null || principal.getUser() == null) {
            throw new BusinessException(AuthErrorCode.USER_UNAUTHORIZED);
        }

        User user = principal.getUser();
        if (user.getUserRole() != UserRole.FP) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
        }

        UUID fpId = user.getId();

        Page<HandoverReceivedItemResponse> page = handoverReceivedQueryRepository.findReceivedHandovers(fpId, pageable);

        return PageResponse.from(page);
    }

    // 인수인계 요청 뮥륙 요약
    @Transactional(readOnly = true)
    public HandoverSummaryResponse getSummary(PrincipalDetails principal, String organizationCode) {
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
        return handoverDetailQueryRepository.findSummary(accessScope, normalizeNullable(organizationCode));
    }

    // 받은 인수인계 목록 요약
    @Transactional(readOnly = true)
    public HandoverReceivedSummaryResponse getReceivedSummary(PrincipalDetails principal) {
        UUID fpId = principal.getUser().getId();
        return handoverReceivedQueryRepository.findReceivedSummary(fpId);
    }

    // 설계사 직접 지정 목록
    @Transactional(readOnly = true)
    public PageResponse<HandoverAssignableFpResponse> getAssignableFps(
            PrincipalDetails principal, UUID handoverRequestId, Pageable pageable) {

        if (principal == null || principal.getUser() == null) {
            throw new BusinessException(AuthErrorCode.USER_UNAUTHORIZED);
        }

        User user = principal.getUser();
        if (user.getUserRole() != UserRole.BRANCH_MANAGER) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
        }
        if (user.getOrganization() == null) {
            throw new BusinessException(AuthErrorCode.INVALID_USER_STATE, "지점장 사용자에 조직 정보가 없습니다.");
        }

        // 1. 요청 조회
        HandoverRequest handoverRequest = handoverRequestRepository.findById(handoverRequestId)
                .orElseThrow(() -> new BusinessException(HandoverErrorCode.HANDOVER_REQUEST_NOT_FOUND));

        // 2. 권한 체크
        User currentFp = handoverRequest.getCurrentFp();
        if (currentFp == null || currentFp.getOrganization() == null) {
            throw new BusinessException(AuthErrorCode.INVALID_USER_STATE, "인수인계 요청의 현재 담당 설계사 또는 조직 정보가 없습니다.");
        }
        if (!currentFp.getOrganization().getId().equals(user.getOrganization().getId())) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
        }

        // 3. 같은 지점 설계사 목록 조회
        String organizationCode = user.getOrganization().getOrganizationCode();
        Page<HandoverAssignableFpResponse> page = handoverDetailQueryRepository
                .findAssignableFps(organizationCode, pageable);
        return PageResponse.from(page);
    }

    // 설계사 직접 지정
    @Transactional
    public void processAssign(PrincipalDetails principal,
                              UUID handoverRequestId,
                              HandoverAssignRequest request) {

        User user = principal.getUser();

        HandoverRequest handoverRequest = handoverRequestRepository.findById(handoverRequestId)
                .orElseThrow(() -> new BusinessException(HandoverErrorCode.HANDOVER_REQUEST_NOT_FOUND));

        if (handoverRequest.getRequestStatus() == RequestStatus.COMPLETED) {
            throw new BusinessException(HandoverErrorCode.HANDOVER_ALREADY_COMPLETED);
        }

        if (user.getUserRole() != UserRole.BRANCH_MANAGER) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
        }
        if (!handoverRequest.getCustomer().getCustomerFp()
                .getOrganization().getId().equals(user.getOrganization().getId())) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
        }

        User assignedFp = userRepository.findById(request.assignedFpId())
                .orElseThrow(() -> new BusinessException(HandoverErrorCode.FP_NOT_FOUND));

        if (assignedFp.getUserRole() != UserRole.FP || !assignedFp.isActive()) {
            throw new BusinessException(HandoverErrorCode.FP_NOT_FOUND);
        }

        if (assignedFp.getOrganization() == null || !assignedFp.getOrganization().getId().equals(user.getOrganization().getId())) {
                        throw new BusinessException(AuthErrorCode.USER_FORBIDDEN, "동일 지점 소속 설계사만 지정할 수 있습니다.");
        }

        List<HandoverRecommendation> pendingRecs = handoverRecommendationRepository
                .findLatestByHandoverRequestAndApprovalStatus(
                        handoverRequest, ApprovalStatus.PENDING, PageRequest.of(0, 1));
        if (!pendingRecs.isEmpty()) {
            pendingRecs.get(0).reject(user.getId());
        }

        HandoverRecommendation newRec = HandoverRecommendation.create(
                handoverRequest, assignedFp, "지점장 직접 지정");
        newRec.approve(user.getId());
        handoverRecommendationRepository.save(newRec);

        Customer customer = handoverRequest.getCustomer();
        customer.changeCustomerFp(assignedFp);

        int nextSequence = customerFpHistoryRepository
                .findMaxCustomerFpSequence(customer.getId()) + 1;

        CustomerFpHistory history = CustomerFpHistory.create(
                customer, handoverRequestId, handoverRequest.getCurrentFp(), assignedFp, "지점장 직접 지정");
        history.applyChangeMetadata(user, nextSequence);
        customerFpHistoryRepository.save(history);

        handoverRequest.complete();

        // SSE 알림
        notificationPublisher.publish(NotificationEvent.builder()
                .receiverUserId(assignedFp.getId())
                .type(NotificationType.HANDOVER_RECEIVED)
                .message(customer.getCustomerName() + " 고객이 담당 고객으로 배정되었습니다.")
                .referenceId(handoverRequest.getId())
                .build());

        // SMS 발송
        eventPublisher.publishEvent(new HandoverSmsEvent(
                customer.getCustomerPhone(),
                assignedFp.getUserName()
        ));
    }

    // 월별 인수인계 추이
    @Transactional(readOnly = true)
    public List<HandoverMonthlyTrendResponse> getMonthlyTrend(PrincipalDetails principal, int trendMonths, String organizationCode) {
        if (trendMonths < 1 || trendMonths > 24) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "trendMonths는 1부터 24 사이여야 합니다.");
        }

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

        LocalDate fromDate = LocalDate.now().minusMonths(trendMonths - 1L).withDayOfMonth(1);
        LocalDate toDate = LocalDate.now().plusMonths(1).withDayOfMonth(1);
        String normalizedOrganizationCode = normalizeNullable(organizationCode);

        Map<String, Long> countByYearMonth = handoverDetailQueryRepository.findMonthlyTrend(
                        accessScope, normalizedOrganizationCode, fromDate, toDate)
                .stream()
                .collect(Collectors.toMap(
                        HandoverMonthlyTrendResponse::yearMonth,
                        HandoverMonthlyTrendResponse::requestCount
                ));

        YearMonth startMonth = YearMonth.from(fromDate);
        return java.util.stream.IntStream.range(0, trendMonths)
                .mapToObj(startMonth::plusMonths)
                .map(yearMonth -> {
                    String key = yearMonth.toString();
                    return new HandoverMonthlyTrendResponse(key, countByYearMonth.getOrDefault(key, 0L));
                })
                .toList();
    }

    // 지점별 현황 조회
    @Transactional(readOnly = true)
    public List<HandoverBranchSummaryResponse> getBranchSummary(PrincipalDetails principal) {
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

        LocalDate fromDate = LocalDate.now().withDayOfMonth(1);
        LocalDate toDate = fromDate.plusMonths(1);
        return handoverDetailQueryRepository.findBranchSummary(accessScope, fromDate, toDate);
    }



    // 인수인계 상세 조회 접근
    private void validateApprovalProcessTarget(HandoverRequest handoverRequest) {
        if (handoverRequest.getRequestStatus() == RequestStatus.COMPLETED) {
            throw new BusinessException(HandoverErrorCode.HANDOVER_ALREADY_COMPLETED);
        }

        if (!isApprovalProcessable(handoverRequest)) {
            throw new BusinessException(HandoverErrorCode.INVALID_HANDOVER_APPROVAL_TARGET);
        }
    }

    private boolean isApprovalProcessable(HandoverRequest handoverRequest) {
        return handoverRequest.getRequestStatus() == RequestStatus.MANAGER_PENDING;
    }

    private User findBranchManager(User recommendedFp) {
        if (recommendedFp == null || recommendedFp.getOrganization() == null) {
            throw new BusinessException(AuthErrorCode.INVALID_USER_STATE, "추천 설계사의 조직 정보가 없습니다.");
        }

        return userRepository.findByOrganizationIdAndUserRoleAndDeletedAtIsNull(
                        recommendedFp.getOrganization().getId(),
                        UserRole.BRANCH_MANAGER
                )
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_USER_STATE, "지점장을 찾을 수 없습니다."));
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


    // 인수인계 요청 생성 접근
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

    // 지점장 접근
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

    // Fp 접근
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

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }

        int age = Period.between(birthDate, LocalDate.now()).getYears();
        return age >= 0 ? age : null;
    }

    private List<HandoverDetailResponse.MatchingReason> parseMatchingReasons(String matchingReasonsJson) {
        if (matchingReasonsJson == null || matchingReasonsJson.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(
                    matchingReasonsJson,
                    new TypeReference<List<HandoverDetailResponse.MatchingReason>>() {}
            );
        } catch (Exception e) {
            return List.of();
        }
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

}
