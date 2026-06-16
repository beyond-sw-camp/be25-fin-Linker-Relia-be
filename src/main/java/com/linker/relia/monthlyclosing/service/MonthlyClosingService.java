package com.linker.relia.monthlyclosing.service;

import com.linker.relia.monthlyclosing.repository.MonthlyClosingCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonthlyClosingService {
    private final MonthlyClosingCommandRepository monthlyClosingCommandRepository;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Transactional
    public void runPreviousMonthClosing(YearMonth closingMonth) {
        if (!running.compareAndSet(false, true)) {
            log.warn("월 마감 작업이 이미 실행 중이어서 건너뜁니다. closingMonth={}", closingMonth);
            return;
        }

        try {
            String closingMonthValue = closingMonth.toString();
            LocalDateTime closedAt = closingMonth.atEndOfMonth().atTime(23, 59, 59);
            LocalDateTime monthStart = closingMonth.atDay(1).atStartOfDay();
            LocalDateTime nextMonthStart = closingMonth.plusMonths(1).atDay(1).atStartOfDay();

            if (monthlyClosingCommandRepository.existsClosingData(closingMonthValue)) {
                log.warn("이미 월 마감 데이터가 존재하여 스케줄러 실행을 건너뜁니다. closingMonth={}", closingMonthValue);
                return;
            }

            log.info("월 마감 작업을 시작합니다. closingMonth={}", closingMonthValue);

            monthlyClosingCommandRepository.insertHrMonthlyClosing(closingMonthValue, closedAt);
            monthlyClosingCommandRepository.insertOrganizationMonthlyClosing(closingMonthValue, closedAt);
            monthlyClosingCommandRepository.insertFpCommissionMonthlyClosing(closingMonthValue, closedAt);
            monthlyClosingCommandRepository.insertBranchCommissionMonthlyClosing(closingMonthValue, closedAt);
            monthlyClosingCommandRepository.insertIncomeCommissionMonthlyClosing(closingMonthValue, closedAt);
            monthlyClosingCommandRepository.insertBranchIncomeCommissionMonthlyClosing(closingMonthValue, closedAt);
            monthlyClosingCommandRepository.insertBranchCustomerMonthlyClosing(closingMonthValue, closedAt);
            monthlyClosingCommandRepository.insertAllBranchCustomerMonthlyClosing(closingMonthValue, closedAt);
            monthlyClosingCommandRepository.insertBranchContractMonthlyClosing(closingMonthValue, closedAt);
            monthlyClosingCommandRepository.insertAllBranchContractMonthlyClosing(closingMonthValue, closedAt);
            monthlyClosingCommandRepository.insertBranchHandoverMonthlyClosing(closingMonthValue, closedAt, monthStart, nextMonthStart);
            monthlyClosingCommandRepository.insertAllBranchHandoverMonthlyClosing(closingMonthValue, closedAt);

            log.info("월 마감 작업을 완료했습니다. closingMonth={}", closingMonthValue);
        } finally {
            running.set(false);
        }
    }
}
