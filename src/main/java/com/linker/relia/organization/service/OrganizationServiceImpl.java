package com.linker.relia.organization.service;

import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.common.access.AccessScope;
import com.linker.relia.common.access.AccessScopeResolver;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.common.exception.CommonErrorCode;
import com.linker.relia.customer.repository.CustomerRepository;
import com.linker.relia.handover.domain.HandoverRequest;
import com.linker.relia.handover.service.HandoverService;
import com.linker.relia.organization.domain.Organization;
import com.linker.relia.organization.dto.BranchOrganizationResponse;
import com.linker.relia.organization.dto.FpContractListRequest;
import com.linker.relia.organization.dto.FpContractListResponse;
import com.linker.relia.organization.dto.FpDetailResponse;
import com.linker.relia.organization.dto.FpListRequest;
import com.linker.relia.organization.dto.FpListResponse;
import com.linker.relia.organization.dto.FpMonthlyPerformanceResponse;
import com.linker.relia.organization.dto.FpResignRequest;
import com.linker.relia.organization.dto.FpResignResponse;
import com.linker.relia.organization.dto.OrganizationChartItemResponse;
import com.linker.relia.organization.dto.OrganizationChartRequest;
import com.linker.relia.organization.dto.OrganizationChartResponse;
import com.linker.relia.organization.dto.OrganizationMemberItemResponse;
import com.linker.relia.organization.dto.OrganizationMemberListRequest;
import com.linker.relia.organization.exception.OrganizationErrorCode;
import com.linker.relia.organization.repository.OrganizationFpRepository;
import com.linker.relia.organization.repository.OrganizationMemberRepository;
import com.linker.relia.organization.repository.OrganizationRepository;
import com.linker.relia.security.principal.PrincipalDetails;
import com.linker.relia.user.domain.User;
import com.linker.relia.user.domain.UserStatus;
import com.linker.relia.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {
    private static final Pattern CLOSING_MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");
    private static final int MAX_FP_CONTRACT_PAGE_SIZE = 200;

    private final OrganizationRepository organizationRepository;
    private final OrganizationFpRepository organizationFpRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final AccessScopeResolver accessScopeResolver;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final HandoverService handoverService;

    @Override
    @Transactional(readOnly = true)
    public List<BranchOrganizationResponse> getBranchOrganizations() {
        return organizationRepository.findBranchOrganizationsWithAdvisorCount();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizationMemberItemResponse> getOrganizationMembers(PrincipalDetails principalDetails,
                                                                       OrganizationMemberListRequest request) {
        AccessScope accessScope = accessScopeResolver.resolve(principalDetails);

        return organizationMemberRepository.searchMembers(
                accessScope,
                normalizeNullable(request.getKeyword()),
                normalizeNullable(request.getBranchKeyword()),
                request.getOrganizationId(),
                request.getRole(),
                request.getStatus(),
                request.getSort(),
                request.toPageable()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationChartResponse getOrganizationChart(OrganizationChartRequest request) {
        List<Organization> organizations = request.getStatus() == null
                ? organizationRepository.findAllByDeletedAtIsNullOrderByCreatedAtAsc()
                : organizationRepository.findAllByOrganizationStatusAndDeletedAtIsNullOrderByCreatedAtAsc(request.getStatus());

        Map<UUID, OrganizationChartItemResponse> organizationMap = new LinkedHashMap<>();
        organizations.forEach(organization ->
                organizationMap.put(organization.getId(), OrganizationChartItemResponse.from(organization)));

        List<OrganizationChartItemResponse> roots = new ArrayList<>();
        organizationMap.values().forEach(organization -> {
            UUID parentOrganizationId = organization.getParentOrganizationId();
            OrganizationChartItemResponse parent = organizationMap.get(parentOrganizationId);

            if (parent == null) {
                roots.add(organization);
                return;
            }

            parent.getChildren().add(organization);
        });

        return OrganizationChartResponse.builder()
                .organizations(roots)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FpListResponse getFps(PrincipalDetails principalDetails, FpListRequest request) {
        Pageable pageable = resolvePageable(request);
        String closingMonth = normalizeClosingMonth(request.getClosingMonth());
        String keyword = normalizeNullable(request.getKeyword());
        AccessScope accessScope = accessScopeResolver.resolve(principalDetails);

        return FpListResponse.from(organizationFpRepository.searchFps(
                accessScope,
                keyword,
                request.getOrganizationId(),
                closingMonth,
                pageable
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public FpDetailResponse getFpDetail(PrincipalDetails principalDetails, UUID fpId, String closingMonth) {
        String normalizedClosingMonth = normalizeClosingMonth(closingMonth);
        AccessScope accessScope = accessScopeResolver.resolve(principalDetails);

        return organizationFpRepository.findFpDetail(accessScope, fpId, normalizedClosingMonth)
                .orElseThrow(() -> resolveFpDetailNotFoundException(fpId));
    }

    @Override
    @Transactional(readOnly = true)
    public FpContractListResponse getFpContracts(PrincipalDetails principalDetails,
                                                 UUID fpId,
                                                 FpContractListRequest request) {
        AccessScope accessScope = accessScopeResolver.resolve(principalDetails);
        validateFpAccessible(accessScope, fpId);
        validateFpContractPageSize(request.getSize());

        return FpContractListResponse.from(organizationFpRepository.findFpContracts(
                accessScope,
                fpId,
                request.toPageable()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public FpMonthlyPerformanceResponse getFpMonthlyPerformances(PrincipalDetails principalDetails,
                                                                 UUID fpId,
                                                                 String fromClosingMonth,
                                                                 String toClosingMonth) {
        String normalizedFromClosingMonth = normalizeClosingMonth(fromClosingMonth);
        String normalizedToClosingMonth = normalizeClosingMonth(toClosingMonth);
        validateClosingMonthRange(normalizedFromClosingMonth, normalizedToClosingMonth);

        AccessScope accessScope = accessScopeResolver.resolve(principalDetails);
        validateFpAccessible(accessScope, fpId);

        return FpMonthlyPerformanceResponse.builder()
                .fpId(fpId)
                .performances(organizationFpRepository.findFpMonthlyPerformances(
                        accessScope,
                        fpId,
                        normalizedFromClosingMonth,
                        normalizedToClosingMonth
                ))
                .build();
    }

    @Override
    @Transactional
    public FpResignResponse resignFp(PrincipalDetails principalDetails, UUID fpId, FpResignRequest request) {
        User fp = userRepository.findByIdForUpdate(fpId)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.FP_NOT_FOUND));

        AccessScope accessScope = accessScopeResolver.resolve(principalDetails);
        validateFpAccessible(accessScope, fpId);

        if (UserStatus.RESIGNED == fp.getUserStatus()) {
            throw new BusinessException(OrganizationErrorCode.FP_ALREADY_RESIGNED);
        }

        // 해촉은 담당 고객 리스트 기준으로 처리한다.
        // 실제 인수인계 요청 생성은 고객 1명씩 HandoverService의 단건 로직을 재사용한다.
        List<HandoverRequest> savedHandoverRequests = customerRepository.findAllByCustomerFpIdAndDeletedAtIsNull(fpId)
                .stream()
                .map(handoverService::createResignationHandoverIfAbsent)
                .flatMap(Optional::stream)
                .toList();

        fp.resign(request.getResignedAt());

        return FpResignResponse.builder()
                .id(fp.getId())
                .userStatus(fp.getUserStatus())
                .resignedAt(fp.getResignedAt())
                .handoverRequestCount(savedHandoverRequests.size())
                .build();
    }

    private void validateFpContractPageSize(Integer size) {
        if (size != null && size > MAX_FP_CONTRACT_PAGE_SIZE) {
            throw new BusinessException(
                    CommonErrorCode.INVALID_REQUEST,
                    "size는 " + MAX_FP_CONTRACT_PAGE_SIZE + " 이하여야 합니다."
            );
        }
    }

    private void validateFpAccessible(AccessScope accessScope, UUID fpId) {
        if (!organizationFpRepository.existsFp(fpId)) {
            throw new BusinessException(OrganizationErrorCode.FP_NOT_FOUND);
        }

        if (!organizationFpRepository.existsFpInScope(accessScope, fpId)) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
        }
    }

    private BusinessException resolveFpDetailNotFoundException(UUID fpId) {
        if (!organizationFpRepository.existsFp(fpId)) {
            return new BusinessException(OrganizationErrorCode.FP_NOT_FOUND);
        }

        return new BusinessException(AuthErrorCode.USER_FORBIDDEN);
    }

    private Pageable resolvePageable(FpListRequest request) {
        boolean pageMissing = request.getPage() == null;
        boolean sizeMissing = request.getSize() == null;

        if (pageMissing && sizeMissing) {
            return Pageable.unpaged();
        }

        if (pageMissing || sizeMissing) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "page와 size는 함께 요청해야 합니다.");
        }

        if (request.getPage() < 1) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "page는 1 이상이어야 합니다.");
        }

        if (request.getSize() < 1) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "size는 1 이상이어야 합니다.");
        }

        return PageRequest.of(request.getPage() - 1, request.getSize());
    }

    private String normalizeClosingMonth(String closingMonth) {
        String normalizedClosingMonth = normalizeNullable(closingMonth);
        if (normalizedClosingMonth == null) {
            return null;
        }

        if (!CLOSING_MONTH_PATTERN.matcher(normalizedClosingMonth).matches()) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "closingMonth는 YYYY-MM 형식이어야 합니다.");
        }

        try {
            YearMonth.parse(normalizedClosingMonth);
            return normalizedClosingMonth;
        } catch (DateTimeParseException exception) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "closingMonth는 YYYY-MM 형식이어야 합니다.");
        }
    }

    private void validateClosingMonthRange(String fromClosingMonth, String toClosingMonth) {
        if (fromClosingMonth == null || toClosingMonth == null) {
            return;
        }

        if (YearMonth.parse(fromClosingMonth).isAfter(YearMonth.parse(toClosingMonth))) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "fromClosingMonth는 toClosingMonth보다 이전이어야 합니다.");
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
