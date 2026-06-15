package com.linker.relia.organization.service;

import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.common.access.AccessScope;
import com.linker.relia.common.access.AccessScopeResolver;
import com.linker.relia.common.access.AccessScopeType;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.organization.dto.FpDetailResponse;
import com.linker.relia.organization.dto.FpListItemResponse;
import com.linker.relia.organization.dto.FpListRequest;
import com.linker.relia.organization.dto.FpListResponse;
import com.linker.relia.organization.exception.OrganizationErrorCode;
import com.linker.relia.organization.repository.OrganizationFpRepository;
import com.linker.relia.organization.repository.OrganizationRepository;
import com.linker.relia.security.principal.PrincipalDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceImplTest {
    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationFpRepository organizationFpRepository;

    @Mock
    private AccessScopeResolver accessScopeResolver;

    private OrganizationServiceImpl organizationService;
    private PrincipalDetails principalDetails;
    private AccessScope accessScope;

    @BeforeEach
    void setUp() {
        organizationService = new OrganizationServiceImpl(
                organizationRepository,
                organizationFpRepository,
                accessScopeResolver
        );
        principalDetails = mock(PrincipalDetails.class);
        accessScope = new AccessScope(AccessScopeType.ALL, UUID.randomUUID(), UUID.randomUUID());
    }

    @Test
    void getFpsReturnsPagedResponse() {
        FpListRequest request = request(1, 10);
        FpListItemResponse item = item();
        when(accessScopeResolver.resolve(principalDetails)).thenReturn(accessScope);
        when(organizationFpRepository.searchFps(eq(accessScope), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item), Pageable.ofSize(10), 1));

        FpListResponse response = organizationService.getFps(principalDetails, request);

        assertThat(response.getContent()).containsExactly(item);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
    }

    @Test
    void getFpsReturnsAllWhenPageAndSizeMissing() {
        FpListRequest request = request(null, null);
        FpListItemResponse item = item();
        when(accessScopeResolver.resolve(principalDetails)).thenReturn(accessScope);
        when(organizationFpRepository.searchFps(eq(accessScope), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item), Pageable.unpaged(), 1));

        FpListResponse response = organizationService.getFps(principalDetails, request);

        assertThat(response.getContent()).containsExactly(item);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
        verify(organizationFpRepository).searchFps(eq(accessScope), eq(null), eq(null), eq(null), eq(Pageable.unpaged()));
    }

    @Test
    void getFpsPassesNormalizedKeyword() {
        FpListRequest request = request(1, 10);
        request.setKeyword("  Planner  ");
        when(accessScopeResolver.resolve(principalDetails)).thenReturn(accessScope);
        when(organizationFpRepository.searchFps(eq(accessScope), eq("Planner"), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(10), 0));

        organizationService.getFps(principalDetails, request);

        verify(organizationFpRepository).searchFps(eq(accessScope), eq("Planner"), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void getFpsPassesOrganizationIdFilter() {
        UUID organizationId = UUID.randomUUID();
        FpListRequest request = request(1, 10);
        request.setOrganizationId(organizationId);
        when(accessScopeResolver.resolve(principalDetails)).thenReturn(accessScope);
        when(organizationFpRepository.searchFps(eq(accessScope), eq(null), eq(organizationId), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(10), 0));

        organizationService.getFps(principalDetails, request);

        verify(organizationFpRepository).searchFps(eq(accessScope), eq(null), eq(organizationId), eq(null), any(Pageable.class));
    }

    @Test
    void getFpsPassesClosingMonthFilter() {
        FpListRequest request = request(1, 10);
        request.setClosingMonth("2026-05");
        when(accessScopeResolver.resolve(principalDetails)).thenReturn(accessScope);
        when(organizationFpRepository.searchFps(eq(accessScope), eq(null), eq(null), eq("2026-05"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(10), 0));

        organizationService.getFps(principalDetails, request);

        verify(organizationFpRepository).searchFps(eq(accessScope), eq(null), eq(null), eq("2026-05"), any(Pageable.class));
    }

    @Test
    void getFpsThrowsWhenOnlyPageOrSizeMissing() {
        FpListRequest onlyPage = request(1, null);
        FpListRequest onlySize = request(null, 10);

        assertThatThrownBy(() -> organizationService.getFps(principalDetails, onlyPage))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> organizationService.getFps(principalDetails, onlySize))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getFpsThrowsWhenPageOrSizeInvalid() {
        FpListRequest invalidPage = request(0, 10);
        FpListRequest invalidSize = request(1, 0);

        assertThatThrownBy(() -> organizationService.getFps(principalDetails, invalidPage))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> organizationService.getFps(principalDetails, invalidSize))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getFpsThrowsWhenClosingMonthInvalid() {
        FpListRequest request = request(1, 10);
        request.setClosingMonth("2026-13");

        assertThatThrownBy(() -> organizationService.getFps(principalDetails, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getFpDetailReturnsResponse() {
        UUID fpId = UUID.randomUUID();
        FpDetailResponse response = FpDetailResponse.builder()
                .fpId(fpId)
                .empCode("FP-0001")
                .fpName("Planner")
                .organizationId(UUID.randomUUID())
                .organizationName("Branch")
                .performanceSummary(FpDetailResponse.PerformanceSummary.builder()
                        .closingMonth("2026-05")
                        .completedContractCount(156)
                        .newContractCount(12)
                        .retentionRate(new BigDecimal("98.50"))
                        .totalRank(4)
                        .branchRank(1)
                        .build())
                .build();
        when(accessScopeResolver.resolve(principalDetails)).thenReturn(accessScope);
        when(organizationFpRepository.findFpDetail(accessScope, fpId, "2026-05")).thenReturn(Optional.of(response));

        FpDetailResponse actual = organizationService.getFpDetail(principalDetails, fpId, "2026-05");

        assertThat(actual).isSameAs(response);
    }

    @Test
    void getFpDetailThrowsNotFoundWhenFpDoesNotExist() {
        UUID fpId = UUID.randomUUID();
        when(accessScopeResolver.resolve(principalDetails)).thenReturn(accessScope);
        when(organizationFpRepository.findFpDetail(accessScope, fpId, null)).thenReturn(Optional.empty());
        when(organizationFpRepository.existsFp(fpId)).thenReturn(false);

        assertThatThrownBy(() -> organizationService.getFpDetail(principalDetails, fpId, null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(OrganizationErrorCode.FP_NOT_FOUND));
    }

    @Test
    void getFpDetailThrowsForbiddenWhenFpExistsOutsideScope() {
        UUID fpId = UUID.randomUUID();
        when(accessScopeResolver.resolve(principalDetails)).thenReturn(accessScope);
        when(organizationFpRepository.findFpDetail(accessScope, fpId, null)).thenReturn(Optional.empty());
        when(organizationFpRepository.existsFp(fpId)).thenReturn(true);

        assertThatThrownBy(() -> organizationService.getFpDetail(principalDetails, fpId, null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.USER_FORBIDDEN));
    }
    private FpListRequest request(Integer page, Integer size) {
        FpListRequest request = new FpListRequest();
        request.setPage(page);
        request.setSize(size);
        return request;
    }

    private FpListItemResponse item() {
        return FpListItemResponse.builder()
                .id(UUID.randomUUID())
                .empCode("FP-0001")
                .userName("Planner")
                .organizationId(UUID.randomUUID())
                .organizationName("Branch")
                .closingMonth("2026-05")
                .customerCount(48)
                .contractCount(21)
                .retentionRate(new BigDecimal("92.50"))
                .build();
    }
}

