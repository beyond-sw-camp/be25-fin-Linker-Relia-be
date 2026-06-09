package com.linker.relia.contract.service;

import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.common.access.AccessScope;
import com.linker.relia.common.access.AccessScopeResolver;
import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.contract.dto.ContractListItemResponse;
import com.linker.relia.contract.dto.ContractListRequest;
import com.linker.relia.contract.dto.ContractSummaryRequest;
import com.linker.relia.contract.dto.ContractSummaryResponse;
import com.linker.relia.contract.repository.ContractRepository;
import com.linker.relia.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {
    private final ContractRepository contractRepository;
    private final AccessScopeResolver accessScopeResolver;

    @Override
    @Transactional(readOnly = true)
    public ContractSummaryResponse getContractSummary(PrincipalDetails principalDetails,
                                                      ContractSummaryRequest request) {
        AccessScope accessScope = accessScopeResolver.resolve(principalDetails);
        String organizationCode = normalizeNullable(request.getOrganizationCode());
        validateOrganizationCodeFilter(accessScope, organizationCode);

        YearMonth closingMonth = resolveClosingMonth(request.getClosingMonth());
        LocalDate referenceDate = LocalDate.now();
        LocalDate dueDateLimit = referenceDate.plusDays(30);

        return contractRepository.summarizeHoldingContracts(
                accessScope,
                organizationCode,
                request.getInsuranceCompanyId(),
                closingMonth.toString(),
                referenceDate,
                dueDateLimit
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContractListItemResponse> getContracts(PrincipalDetails principalDetails,
                                                               ContractListRequest request) {
        AccessScope accessScope = accessScopeResolver.resolve(principalDetails);
        String organizationCode = normalizeNullable(request.getOrganizationCode());
        validateOrganizationCodeFilter(accessScope, organizationCode);

        YearMonth closingMonth = resolveClosingMonth(request.getClosingMonth());
        LocalDate referenceDate = LocalDate.now();
        LocalDate dueDateLimit = referenceDate.plusDays(30);

        return PageResponse.from(contractRepository.searchHoldingContracts(
                accessScope,
                organizationCode,
                request.getInsuranceCompanyId(),
                closingMonth.toString(),
                request.getContractStatus(),
                request.getSort(),
                referenceDate,
                dueDateLimit,
                request.toPageable()
        ));
    }

    private YearMonth resolveClosingMonth(String closingMonth) {
        String normalizedClosingMonth = normalizeNullable(closingMonth);
        if (normalizedClosingMonth == null) {
            return YearMonth.now().minusMonths(1);
        }
        return YearMonth.parse(normalizedClosingMonth);
    }

    private void validateOrganizationCodeFilter(AccessScope accessScope, String organizationCode) {
        if (organizationCode == null) {
            return;
        }

        if (!accessScope.isAllScope()) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN, "조직 코드로 계약을 조회할 수 있는 권한이 없습니다.");
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
