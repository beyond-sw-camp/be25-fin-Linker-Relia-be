package com.linker.relia.handover.service;

import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.common.access.AccessScope;
import com.linker.relia.common.access.AccessScopeType;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.customer.domain.Customer;
import com.linker.relia.customer.repository.CustomerRepository;
import com.linker.relia.handover.domain.HandoverRecommendation;
import com.linker.relia.handover.domain.HandoverRequest;
import com.linker.relia.handover.domain.RequestStatus;
import com.linker.relia.handover.domain.RequestType;
import com.linker.relia.handover.dto.request.HandoverCreateRequest;
import com.linker.relia.handover.dto.response.HandoverCreateResponse;
import com.linker.relia.handover.dto.response.HandoverListItemResponse;
import com.linker.relia.handover.dto.response.HandoverListResponse;
import com.linker.relia.handover.exception.HandoverErrorCode;
import com.linker.relia.handover.repository.HandoverRecommendationRepository;
import com.linker.relia.handover.repository.HandoverRequestRepository;
import com.linker.relia.security.principal.PrincipalDetails;
import com.linker.relia.user.domain.User;
import com.linker.relia.user.repository.UserRepository;
import jakarta.persistence.Access;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class HandoverService {

    private final HandoverRequestRepository handoverRequestRepository;
    private final HandoverRecommendationRepository handoverRecommendationRepository;
    private final CustomerRepository customerRepository;
    private final RecommendationService recommendationService;

    public HandoverCreateResponse createHandover(HandoverCreateRequest request) {

        // 1. 고객 조회
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new BusinessException(HandoverErrorCode.CUSTOMER_NOT_FOUND));

        // 2. 중복 요청 체크
        boolean exists = handoverRequestRepository.existsByCustomerAndRequestStatusIn(
                customer,
                List.of(RequestStatus.MANAGER_PENDING, RequestStatus.RETRY)
        );
        if (exists) {
            throw new BusinessException(HandoverErrorCode.HANDOVER_ALREADY_IN_PROGRESS);
        }

        // 3. 인수인계 요청 생성 및 저장
        HandoverRequest handoverRequest = HandoverRequest.create(customer, request.requestType());
        handoverRequestRepository.save(handoverRequest);

        // 4. 추천 스코어링 (동기)
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
        AccessScope accessScope = switch (user.getUserRole()){
            case BRANCH_MANAGER -> new AccessScope(AccessScopeType.BRANCH, user.getId(), user.getOrganization().getId());
            default -> new AccessScope(AccessScopeType.ALL, user.getId(),null);
        };

        Page<HandoverListItemResponse> page = handoverRequestRepository
                .searchHandovers(accessScope, status, requestType, customerName, pageable);

        return HandoverListResponse.of(page);
    }
}
