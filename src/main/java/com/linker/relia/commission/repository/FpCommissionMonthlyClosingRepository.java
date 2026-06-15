package com.linker.relia.commission.repository;

import com.linker.relia.commission.domain.FpCommissionMonthlyClosing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FpCommissionMonthlyClosingRepository extends JpaRepository<FpCommissionMonthlyClosing, UUID> {
    Optional<FpCommissionMonthlyClosing> findByFp_IdAndClosingMonth(UUID fpId, String closingMonth);
}
