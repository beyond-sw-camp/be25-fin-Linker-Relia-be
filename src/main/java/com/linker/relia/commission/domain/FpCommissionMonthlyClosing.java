package com.linker.relia.commission.domain;

import com.linker.relia.organization.domain.Organization;
import com.linker.relia.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fp_commission_monthly_closing")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FpCommissionMonthlyClosing {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @Column(name = "closing_month")
    private String closingMonth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fp_id", nullable = false)
    private User fp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "total_initial_payment_amount", nullable = false)
    private BigDecimal totalInitialPaymentAmount;

    @Column(name = "total_maintenance_payment_amount", nullable = false)
    private BigDecimal totalMaintenancePaymentAmount;

    @Column(name = "total_recovery_collection_amount", nullable = false)
    private BigDecimal totalRecoveryCollectionAmount;

    @Column(name = "total_payment_amount", nullable = false)
    private BigDecimal totalPaymentAmount;

    @Column(name = "net_commission_amount", nullable = false)
    private BigDecimal netCommissionAmount;

    @Column(name = "contract_count", nullable = false)
    private int contractCount;

    @Column(name = "recovery_contract_count", nullable = false)
    private int recoveryContractCount;

    @Column(name = "closed_at", nullable = false)
    private LocalDateTime closedAt;
}
