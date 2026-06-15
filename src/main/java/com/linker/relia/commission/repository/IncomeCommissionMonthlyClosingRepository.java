package com.linker.relia.commission.repository;

import com.linker.relia.commission.domain.IncomeCommissionMonthlyClosing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncomeCommissionMonthlyClosingRepository extends JpaRepository<IncomeCommissionMonthlyClosing, UUID> {
    Optional<IncomeCommissionMonthlyClosing> findByClosingMonth(String closingMonth);
}
