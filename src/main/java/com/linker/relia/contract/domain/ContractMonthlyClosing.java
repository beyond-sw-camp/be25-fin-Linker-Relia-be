package com.linker.relia.contract.domain;

import com.linker.relia.customer.domain.Customer;
import com.linker.relia.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "contract_monthly_closing")
public class ContractMonthlyClosing {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @Column(name = "closing_month")
    private String closingMonth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @Column(name = "contract_status")
    private String contractStatus;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "current_payment_round")
    private Integer currentPaymentRound;

    @Column(name = "maintenance_round")
    private Integer maintenanceRound;

    @Column(name = "lapse_yn")
    private boolean lapseYn;

    @Column(name = "lapse_at")
    private LocalDate lapseAt;

    @Column(name = "terminated_yn")
    private boolean terminatedYn;

    @Column(name = "terminated_at")
    private LocalDate terminatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fp_id")
    private User fp;

    @Column(name = "contract_date")
    private LocalDate contractDate;

    @Column(name = "contract_start_date")
    private LocalDate contractStartDate;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @Column(name = "payment_period_years")
    private int paymentPeriodYears;

    @Column(name = "payment_cycle")
    private String paymentCycle;

    @Column(name = "monthly_premium")
    private BigDecimal monthlyPremium;

    @Column(name = "coverage_start_date")
    private LocalDate coverageStartDate;

    @Column(name = "coverage_end_date")
    private LocalDate coverageEndDate;

    @Column(name = "coverage_summary")
    private String coverageSummary;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}
