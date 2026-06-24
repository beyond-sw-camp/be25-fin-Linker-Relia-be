package com.linker.relia.hr.service;

import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.commission.repository.IncomeCommissionMonthlyClosingRepository;
import com.linker.relia.common.exception.CommonErrorCode;
import com.linker.relia.hr.domain.HrMonthlyClosing;
import com.linker.relia.hr.domain.OrganizationMonthlyClosing;
import com.linker.relia.hr.dto.HrClosingProcessRequest;
import com.linker.relia.hr.dto.HrClosingProcessResponse;
import com.linker.relia.hr.dto.HrClosingSummaryResponse;
import com.linker.relia.hr.dto.HrClosingUserItemResponse;
import com.linker.relia.hr.dto.HrClosingUserListResponse;
import com.linker.relia.hr.dto.OrganizationClosingItemResponse;
import com.linker.relia.hr.dto.OrganizationClosingListResponse;
import com.linker.relia.hr.exception.HrClosingErrorCode;
import com.linker.relia.hr.repository.HrMonthlyClosingRepository;
import com.linker.relia.hr.repository.OrganizationMonthlyClosingRepository;
import com.linker.relia.organization.domain.Organization;
import com.linker.relia.organization.repository.OrganizationRepository;
import com.linker.relia.user.domain.User;
import com.linker.relia.user.domain.UserRole;
import com.linker.relia.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class HrClosingServiceImpl implements HrClosingService {
    private static final Pattern CLOSING_MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private final HrMonthlyClosingRepository hrMonthlyClosingRepository;
    private final OrganizationMonthlyClosingRepository organizationMonthlyClosingRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final IncomeCommissionMonthlyClosingRepository incomeCommissionMonthlyClosingRepository;

    @Override
    @Transactional(readOnly = true)
    public HrClosingSummaryResponse getSummary(String closingMonth) {
        String normalizedClosingMonth = normalizeClosingMonth(closingMonth);
        List<HrMonthlyClosing> hrClosings = hrMonthlyClosingRepository
                .findAllByClosingMonthOrderByEmpCodeAsc(normalizedClosingMonth);
        List<OrganizationMonthlyClosing> organizationClosings = organizationMonthlyClosingRepository
                .findAllByClosingMonthOrderByOrganizationCodeAsc(normalizedClosingMonth);

        LocalDateTime closedAt = organizationClosings.stream()
                .map(OrganizationMonthlyClosing::getClosedAt)
                .findFirst()
                .orElseGet(() -> hrClosings.stream()
                        .map(HrMonthlyClosing::getClosedAt)
                        .findFirst()
                        .orElse(null));

        return HrClosingSummaryResponse.builder()
                .closingMonth(normalizedClosingMonth)
                .closed(!hrClosings.isEmpty() || !organizationClosings.isEmpty())
                .organizationCount(organizationClosings.size())
                .userCount(hrClosings.size())
                .fpCount(hrClosings.stream().filter(closing -> closing.getUserRole() == UserRole.FP).count())
                .closedAt(closedAt)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationClosingListResponse getOrganizations(String closingMonth) {
        String normalizedClosingMonth = normalizeClosingMonth(closingMonth);
        List<OrganizationClosingItemResponse> organizations = organizationMonthlyClosingRepository
                .findAllByClosingMonthOrderByOrganizationCodeAsc(normalizedClosingMonth)
                .stream()
                .map(OrganizationClosingItemResponse::from)
                .toList();

        return OrganizationClosingListResponse.of(normalizedClosingMonth, organizations);
    }

    @Override
    @Transactional(readOnly = true)
    public HrClosingUserListResponse getUsers(String closingMonth) {
        String normalizedClosingMonth = normalizeClosingMonth(closingMonth);
        List<HrClosingUserItemResponse> users = hrMonthlyClosingRepository
                .findAllByClosingMonthOrderByEmpCodeAsc(normalizedClosingMonth)
                .stream()
                .map(HrClosingUserItemResponse::from)
                .toList();

        return HrClosingUserListResponse.of(normalizedClosingMonth, users);
    }

    @Override
    @Transactional
    public HrClosingProcessResponse close(HrClosingProcessRequest request) {
        String closingMonth = normalizeClosingMonth(request.getClosingMonth());
        validateClosableMonth(closingMonth);
        validateCommissionNotClosed(closingMonth);
        validateNotClosed(closingMonth);

        LocalDateTime closedAt = LocalDateTime.now();
        List<Organization> organizations = organizationRepository.findAllByDeletedAtIsNullOrderByCreatedAtAsc();
        List<User> users = userRepository.findAllByDeletedAtIsNullOrderByEmpCodeAsc();

        List<OrganizationMonthlyClosing> organizationClosings = organizations.stream()
                .map(organization -> OrganizationMonthlyClosing.snapshot(closingMonth, organization, closedAt))
                .toList();
        List<HrMonthlyClosing> hrClosings = users.stream()
                .map(user -> HrMonthlyClosing.snapshot(closingMonth, user, closedAt))
                .toList();

        organizationMonthlyClosingRepository.saveAll(organizationClosings);
        hrMonthlyClosingRepository.saveAll(hrClosings);

        return HrClosingProcessResponse.builder()
                .closingMonth(closingMonth)
                .organizationCount(organizationClosings.size())
                .userCount(hrClosings.size())
                .fpCount(users.stream().filter(user -> user.getUserRole() == UserRole.FP).count())
                .closedAt(closedAt)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isClosed(String closingMonth) {
        String normalizedClosingMonth = normalizeClosingMonth(closingMonth);
        return hrMonthlyClosingRepository.existsByClosingMonth(normalizedClosingMonth)
                || organizationMonthlyClosingRepository.existsByClosingMonth(normalizedClosingMonth);
    }


    private void validateClosableMonth(String closingMonth) {
        YearMonth requestedClosingMonth = YearMonth.parse(closingMonth);
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);

        if (!requestedClosingMonth.isBefore(currentMonth)) {
            throw new BusinessException(HrClosingErrorCode.HR_CLOSING_MONTH_NOT_ALLOWED);
        }
    }

    private void validateCommissionNotClosed(String closingMonth) {
        if (incomeCommissionMonthlyClosingRepository.existsByClosingMonth(closingMonth)) {
            throw new BusinessException(HrClosingErrorCode.HR_CLOSING_COMMISSION_ALREADY_CLOSED);
        }
    }
    private void validateNotClosed(String closingMonth) {
        if (hrMonthlyClosingRepository.existsByClosingMonth(closingMonth)
                || organizationMonthlyClosingRepository.existsByClosingMonth(closingMonth)) {
            throw new BusinessException(HrClosingErrorCode.HR_CLOSING_ALREADY_EXISTS);
        }
    }

    private String normalizeClosingMonth(String closingMonth) {
        if (closingMonth == null || closingMonth.trim().isEmpty()) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "closingMonth는 YYYY-MM 형식이어야 합니다.");
        }

        String normalizedClosingMonth = closingMonth.trim();
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
}

