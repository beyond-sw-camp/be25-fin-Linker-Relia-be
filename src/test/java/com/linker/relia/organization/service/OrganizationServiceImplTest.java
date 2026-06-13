package com.linker.relia.organization.service;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.common.access.AccessScopeResolver;
import com.linker.relia.common.access.AccessScopeType;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.organization.dto.FpListItemResponse;
import com.linker.relia.organization.dto.FpListRequest;
import com.linker.relia.organization.dto.FpListResponse;
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
                .thenReturn(new PageImpl<>(List.of(item), request.toPageable(), 1));

        FpListResponse response = organizationService.getFps(principalDetails, request);

        assertThat(response.getContent()).containsExactly(item);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
    }

    @Test
    void getFpsPassesNormalizedKeyword() {
        FpListRequest request = request(1, 10);
        request.setKeyword("  김설계  ");
        when(accessScopeResolver.resolve(principalDetails)).thenReturn(accessScope);
        when(organizationFpRepository.searchFps(eq(accessScope), eq("김설계"), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), request.toPageable(), 0));

        organizationService.getFps(principalDetails, request);

        verify(organizationFpRepository).searchFps(eq(accessScope), eq("김설계"), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void getFpsPassesOrganizationIdFilter() {
        UUID organizationId = UUID.randomUUID();
        FpListRequest request = request(1, 10);
        request.setOrganizationId(organizationId);
        when(accessScopeResolver.resolve(principalDetails)).thenReturn(accessScope);
        when(organizationFpRepository.searchFps(eq(accessScope), eq(null), eq(organizationId), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), request.toPageable(), 0));

        organizationService.getFps(principalDetails, request);

        verify(organizationFpRepository).searchFps(eq(accessScope), eq(null), eq(organizationId), eq(null), any(Pageable.class));
    }

    @Test
    void getFpsPassesClosingMonthFilter() {
        FpListRequest request = request(1, 10);
        request.setClosingMonth("2026-05");
        when(accessScopeResolver.resolve(principalDetails)).thenReturn(accessScope);
        when(organizationFpRepository.searchFps(eq(accessScope), eq(null), eq(null), eq("2026-05"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), request.toPageable(), 0));

        organizationService.getFps(principalDetails, request);

        verify(organizationFpRepository).searchFps(eq(accessScope), eq(null), eq(null), eq("2026-05"), any(Pageable.class));
    }

    @Test
    void getFpsThrowsWhenPageOrSizeInvalid() {
        FpListRequest invalidPage = request(0, 10);
        FpListRequest invalidSize = request(1, 0);

        assertThatThrownBy(() -> organizationService.getFps(principalDetails, invalidPage))
                .isInstanceOf(BusinessException.class)
                .hasMessage("page는 1 이상이어야 합니다.");
        assertThatThrownBy(() -> organizationService.getFps(principalDetails, invalidSize))
                .isInstanceOf(BusinessException.class)
                .hasMessage("size는 1 이상이어야 합니다.");
    }

    @Test
    void getFpsThrowsWhenClosingMonthInvalid() {
        FpListRequest request = request(1, 10);
        request.setClosingMonth("2026-13");

        assertThatThrownBy(() -> organizationService.getFps(principalDetails, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("closingMonth는 YYYY-MM 형식이어야 합니다.");
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
                .userName("김설계")
                .organizationId(UUID.randomUUID())
                .organizationName("강남지점")
                .closingMonth("2026-05")
                .customerCount(48)
                .contractCount(21)
                .retentionRate(new BigDecimal("92.50"))
                .build();
    }
}