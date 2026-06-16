package com.linker.relia.monthlyclosing.repository;

import java.time.LocalDateTime;

public interface MonthlyClosingCommandRepository {
    boolean existsClosingData(String closingMonth);

    void deleteExistingClosingData(String closingMonth);

    void insertHrMonthlyClosing(String closingMonth, LocalDateTime closedAt);

    void insertOrganizationMonthlyClosing(String closingMonth, LocalDateTime closedAt);

    void insertFpCommissionMonthlyClosing(String closingMonth, LocalDateTime closedAt);

    void insertBranchCommissionMonthlyClosing(String closingMonth, LocalDateTime closedAt);

    void insertIncomeCommissionMonthlyClosing(String closingMonth, LocalDateTime closedAt);

    void insertBranchIncomeCommissionMonthlyClosing(String closingMonth, LocalDateTime closedAt);

    void insertFpMonthlyPerformanceClosing(String closingMonth, LocalDateTime closedAt, LocalDateTime monthStart, LocalDateTime nextMonthStart);

    void insertBranchCustomerMonthlyClosing(String closingMonth, LocalDateTime closedAt);

    void insertAllBranchCustomerMonthlyClosing(String closingMonth, LocalDateTime closedAt);

    void insertBranchContractMonthlyClosing(String closingMonth, LocalDateTime closedAt);

    void insertAllBranchContractMonthlyClosing(String closingMonth, LocalDateTime closedAt);

    void insertBranchHandoverMonthlyClosing(String closingMonth, LocalDateTime closedAt, LocalDateTime monthStart, LocalDateTime nextMonthStart);

    void insertAllBranchHandoverMonthlyClosing(String closingMonth, LocalDateTime closedAt);
}
