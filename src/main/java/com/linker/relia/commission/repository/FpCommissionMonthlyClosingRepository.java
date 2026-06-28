package com.linker.relia.commission.repository;

import com.linker.relia.commission.domain.FpCommissionMonthlyClosing;
import com.linker.relia.commission.dto.FpCommissionListQueryResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface FpCommissionMonthlyClosingRepository extends JpaRepository<FpCommissionMonthlyClosing, UUID> {
    Optional<FpCommissionMonthlyClosing> findByFp_IdAndClosingMonth(UUID fpId, String closingMonth);

    @Query(value = """
            select new com.linker.relia.commission.dto.FpCommissionListQueryResult(
                fp.id,
                fp.userName,
                fcmc.totalInitialPaymentAmount,
                fcmc.totalMaintenancePaymentAmount,
                fcmc.totalRecoveryCollectionAmount,
                fcmc.totalPaymentAmount,
                fcmc.netCommissionAmount,
                fcmc.contractCount,
                fcmc.recoveryContractCount
            )
            from FpCommissionMonthlyClosing fcmc
            join fcmc.fp fp
            join fcmc.organization org
            where fcmc.closingMonth = :closingMonth
              and org.id = :organizationId
              and fp.deletedAt is null
              and org.deletedAt is null
            order by fp.userName asc, fp.id asc
            """,
            countQuery = """
            select count(fcmc)
            from FpCommissionMonthlyClosing fcmc
            join fcmc.fp fp
            join fcmc.organization org
            where fcmc.closingMonth = :closingMonth
              and org.id = :organizationId
              and fp.deletedAt is null
              and org.deletedAt is null
            """)
    Page<FpCommissionListQueryResult> findBranchFpCommissionList(@Param("closingMonth") String closingMonth,
                                                                 @Param("organizationId") UUID organizationId,
                                                                 Pageable pageable);

    @Query(value = """
            select new com.linker.relia.commission.dto.FpCommissionListQueryResult(
                fp.id,
                fp.userName,
                fcmc.totalInitialPaymentAmount,
                fcmc.totalMaintenancePaymentAmount,
                fcmc.totalRecoveryCollectionAmount,
                fcmc.totalPaymentAmount,
                fcmc.netCommissionAmount,
                fcmc.contractCount,
                fcmc.recoveryContractCount
            )
            from FpCommissionMonthlyClosing fcmc
            join fcmc.fp fp
            join fcmc.organization org
            where fcmc.closingMonth = :closingMonth
              and fp.deletedAt is null
              and org.deletedAt is null
            order by fp.userName asc, fp.id asc
            """,
            countQuery = """
            select count(fcmc)
            from FpCommissionMonthlyClosing fcmc
            join fcmc.fp fp
            join fcmc.organization org
            where fcmc.closingMonth = :closingMonth
              and fp.deletedAt is null
              and org.deletedAt is null
            """)
    Page<FpCommissionListQueryResult> findHqFpCommissionList(@Param("closingMonth") String closingMonth,
                                                             Pageable pageable);

    @Query("""
            select new com.linker.relia.commission.dto.FpCommissionListQueryResult(
                fp.id,
                fp.userName,
                fcmc.totalInitialPaymentAmount,
                fcmc.totalMaintenancePaymentAmount,
                fcmc.totalRecoveryCollectionAmount,
                fcmc.totalPaymentAmount,
                fcmc.netCommissionAmount,
                fcmc.contractCount,
                fcmc.recoveryContractCount
            )
            from FpCommissionMonthlyClosing fcmc
            join fcmc.fp fp
            join fcmc.organization org
            where fcmc.closingMonth = :closingMonth
              and fp.deletedAt is null
              and org.deletedAt is null
            order by org.organizationName asc, fp.userName asc, fp.id asc
            """)
    List<FpCommissionListQueryResult> findHqFpCommissionStatementRows(@Param("closingMonth") String closingMonth);

    @Query("""
            select new com.linker.relia.commission.dto.FpCommissionListQueryResult(
                fp.id,
                fp.userName,
                fcmc.totalInitialPaymentAmount,
                fcmc.totalMaintenancePaymentAmount,
                fcmc.totalRecoveryCollectionAmount,
                fcmc.totalPaymentAmount,
                fcmc.netCommissionAmount,
                fcmc.contractCount,
                fcmc.recoveryContractCount
            )
            from FpCommissionMonthlyClosing fcmc
            join fcmc.fp fp
            join fcmc.organization org
            where fcmc.closingMonth = :closingMonth
              and org.id = :organizationId
              and fp.deletedAt is null
              and org.deletedAt is null
            order by fp.userName asc, fp.id asc
            """)
    List<FpCommissionListQueryResult> findBranchFpCommissionStatementRows(@Param("closingMonth") String closingMonth,
                                                                          @Param("organizationId") UUID organizationId);
}
