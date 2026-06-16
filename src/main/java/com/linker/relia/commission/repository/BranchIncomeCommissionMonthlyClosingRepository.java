package com.linker.relia.commission.repository;

import com.linker.relia.commission.domain.BranchIncomeCommissionMonthlyClosing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BranchIncomeCommissionMonthlyClosingRepository extends JpaRepository<BranchIncomeCommissionMonthlyClosing, UUID> {
    Optional<BranchIncomeCommissionMonthlyClosing> findByOrganization_IdAndClosingMonth(UUID organizationId, String closingMonth);
}
