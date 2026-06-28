package com.linker.relia.commission.repository;

import com.linker.relia.commission.domain.BranchCommissionMonthlyClosing;
import com.linker.relia.commission.dto.OrganizationCommissionListQueryResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface BranchCommissionMonthlyClosingRepository extends JpaRepository<BranchCommissionMonthlyClosing, UUID> {
    Optional<BranchCommissionMonthlyClosing> findByOrganization_IdAndClosingMonth(UUID organizationId, String closingMonth);

    @Query(value = """
            select new com.linker.relia.commission.dto.OrganizationCommissionListQueryResult(
                org.id,
                org.organizationName,
                bcmc.totalInitialPaymentAmount,
                bcmc.totalMaintenancePaymentAmount,
                bcmc.totalRecoveryCollectionAmount,
                bcmc.totalPaymentAmount,
                bcmc.netCommissionAmount,
                bcmc.fpCount,
                bcmc.contractCount,
                bcmc.recoveryContractCount
            )
            from BranchCommissionMonthlyClosing bcmc
            join bcmc.organization org
            where bcmc.closingMonth = :closingMonth
              and org.deletedAt is null
              and org.id = :organizationId
            order by org.organizationName asc, org.id asc
            """,
            countQuery = """
            select count(bcmc)
            from BranchCommissionMonthlyClosing bcmc
            join bcmc.organization org
            where bcmc.closingMonth = :closingMonth
              and org.deletedAt is null
              and org.id = :organizationId
            """)
    Page<OrganizationCommissionListQueryResult> findBranchOrganizationCommissionList(@Param("closingMonth") String closingMonth,
                                                                                     @Param("organizationId") UUID organizationId,
                                                                                     Pageable pageable);

    @Query(value = """
            select new com.linker.relia.commission.dto.OrganizationCommissionListQueryResult(
                org.id,
                org.organizationName,
                bcmc.totalInitialPaymentAmount,
                bcmc.totalMaintenancePaymentAmount,
                bcmc.totalRecoveryCollectionAmount,
                bcmc.totalPaymentAmount,
                bcmc.netCommissionAmount,
                bcmc.fpCount,
                bcmc.contractCount,
                bcmc.recoveryContractCount
            )
            from BranchCommissionMonthlyClosing bcmc
            join bcmc.organization org
            where bcmc.closingMonth = :closingMonth
              and org.deletedAt is null
            order by org.organizationName asc, org.id asc
            """,
            countQuery = """
            select count(bcmc)
            from BranchCommissionMonthlyClosing bcmc
            join bcmc.organization org
            where bcmc.closingMonth = :closingMonth
              and org.deletedAt is null
            """)
    Page<OrganizationCommissionListQueryResult> findHqOrganizationCommissionList(@Param("closingMonth") String closingMonth,
                                                                                 Pageable pageable);

    @Query("""
            select new com.linker.relia.commission.dto.OrganizationCommissionListQueryResult(
                org.id,
                org.organizationName,
                bcmc.totalInitialPaymentAmount,
                bcmc.totalMaintenancePaymentAmount,
                bcmc.totalRecoveryCollectionAmount,
                bcmc.totalPaymentAmount,
                bcmc.netCommissionAmount,
                bcmc.fpCount,
                bcmc.contractCount,
                bcmc.recoveryContractCount
            )
            from BranchCommissionMonthlyClosing bcmc
            join bcmc.organization org
            where bcmc.closingMonth = :closingMonth
              and org.deletedAt is null
            order by org.organizationName asc, org.id asc
            """)
    List<OrganizationCommissionListQueryResult> findHqOrganizationCommissionStatementRows(
            @Param("closingMonth") String closingMonth
    );
}
