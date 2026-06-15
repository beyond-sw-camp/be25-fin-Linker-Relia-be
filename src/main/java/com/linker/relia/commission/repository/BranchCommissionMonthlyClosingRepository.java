package com.linker.relia.commission.repository;

import com.linker.relia.commission.domain.BranchCommissionMonthlyClosing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BranchCommissionMonthlyClosingRepository extends JpaRepository<BranchCommissionMonthlyClosing, UUID> {
    Optional<BranchCommissionMonthlyClosing> findByOrganization_IdAndClosingMonth(UUID organizationId, String closingMonth);
}
