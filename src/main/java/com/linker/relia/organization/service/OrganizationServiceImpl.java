package com.linker.relia.organization.service;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.common.access.AccessScopeResolver;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.common.exception.CommonErrorCode;
import com.linker.relia.organization.domain.Organization;
import com.linker.relia.organization.domain.OrganizationStatus;
import com.linker.relia.organization.domain.OrganizationType;
import com.linker.relia.organization.dto.BranchOrganizationResponse;
import com.linker.relia.organization.dto.FpListRequest;
import com.linker.relia.organization.dto.FpListResponse;
import com.linker.relia.organization.dto.OrganizationChartItemResponse;
import com.linker.relia.organization.dto.OrganizationChartRequest;
import com.linker.relia.organization.dto.OrganizationChartResponse;
import com.linker.relia.organization.repository.OrganizationFpRepository;
import com.linker.relia.organization.repository.OrganizationRepository;
import com.linker.relia.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {
    private static final Pattern CLOSING_MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private final OrganizationRepository organizationRepository;
    private final OrganizationFpRepository organizationFpRepository;
    private final AccessScopeResolver accessScopeResolver;

    @Override
    @Transactional(readOnly = true)
    public List<BranchOrganizationResponse> getBranchOrganizations() {
        return organizationRepository
                .findAllByOrganizationTypeAndOrganizationStatusAndDeletedAtIsNullOrderByCreatedAtAsc(
                        OrganizationType.BRANCH,
                        OrganizationStatus.ACTIVE
                )
                .stream()
                .map(BranchOrganizationResponse::from)
                .toList();
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
        validatePageRequest(request);
        String closingMonth = normalizeClosingMonth(request.getClosingMonth());
        String keyword = normalizeNullable(request.getKeyword());
        Pageable pageable = request.toPageable();
        AccessScope accessScope = accessScopeResolver.resolve(principalDetails);

        return FpListResponse.from(organizationFpRepository.searchFps(
                accessScope,
                keyword,
                request.getOrganizationId(),
                closingMonth,
                pageable
        ));
    }

    private void validatePageRequest(FpListRequest request) {
        if (request.getPage() == null || request.getPage() < 1) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "page는 1 이상이어야 합니다.");
        }

        if (request.getSize() == null || request.getSize() < 1) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "size는 1 이상이어야 합니다.");
        }
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

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
