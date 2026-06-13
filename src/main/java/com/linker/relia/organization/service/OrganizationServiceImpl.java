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

    /**
     * Retrieve active, non-deleted branch organizations ordered by creation time.
     *
     * @return a list of BranchOrganizationResponse representing active branch organizations ordered by createdAt ascending
     */
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

    /**
     * Builds a hierarchical organization chart from stored organizations, optionally filtered by status.
     *
     * @param request request containing an optional status filter; when `status` is null all non-deleted organizations are loaded
     * @return an OrganizationChartResponse whose `organizations` are the root nodes, each containing nested child organizations forming the tree
     */
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

    /**
     * Retrieves a paginated list of financial planners (FPs) matching the supplied request and the caller's access scope.
     *
     * @param principalDetails security principal used to resolve the caller's access scope
     * @param request          pagination and filter criteria for the FP search
     * @return                 an FpListResponse containing the paginated FP search results
     */
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

    /**
     * Validate pagination parameters of an FpListRequest.
     *
     * @param request the request whose `page` and `size` fields are validated
     * @throws BusinessException with CommonErrorCode.INVALID_REQUEST if `page` is null or less than 1, or if `size` is null or less than 1
     */
    private void validatePageRequest(FpListRequest request) {
        if (request.getPage() == null || request.getPage() < 1) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "page는 1 이상이어야 합니다.");
        }

        if (request.getSize() == null || request.getSize() < 1) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "size는 1 이상이어야 합니다.");
        }
    }

    /**
     * Validate and normalize a closing month string to the `YYYY-MM` format, or return null when input is blank.
     *
     * @param closingMonth the raw closing month value which may be null or contain surrounding whitespace
     * @return the trimmed `YYYY-MM` string when valid, or `null` if the input is null or empty after trimming
     * @throws BusinessException if the value is non-null/non-empty but does not match the `YYYY-MM` format or cannot be parsed as a YearMonth
     */
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

    /**
     * Trim the input string and convert null or all-whitespace inputs to {@code null}.
     *
     * @param value the string to normalize; may be {@code null}
     * @return {@code null} if {@code value} is {@code null} or contains only whitespace, otherwise the trimmed string
     */
    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
