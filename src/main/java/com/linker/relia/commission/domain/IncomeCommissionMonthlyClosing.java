package com.linker.relia.commission.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "income_commission_monthly_closing")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IncomeCommissionMonthlyClosing {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @Column(name = "closing_month")
    private String closingMonth;

    @Column(name = "net_income_commission_amount", nullable = false)
    private BigDecimal netIncomeCommissionAmount;

    @Column(name = "total_initial_gross_commission_amount", nullable = false)
    private BigDecimal totalInitialGrossCommissionAmount;

    @Column(name = "total_maintenance_gross_commission_amount", nullable = false)
    private BigDecimal totalMaintenanceGrossCommissionAmount;

    @Column(name = "total_payment_commission_amount", nullable = false)
    private BigDecimal totalPaymentCommissionAmount;

    @Column(name = "total_insurance_recovery_amount", nullable = false)
    private BigDecimal totalInsuranceRecoveryAmount;

    @Column(name = "total_fp_recovery_collection_amount", nullable = false)
    private BigDecimal totalFpRecoveryCollectionAmount;

    @Column(name = "closed_at", nullable = false)
    private LocalDateTime closedAt;
}
