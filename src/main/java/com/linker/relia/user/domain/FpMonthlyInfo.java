package com.linker.relia.user.domain;

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

// --------------------------------------------------------------- 임시
@Entity
@Table(name = "fp_monthly_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FpMonthlyInfo {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @Column(name = "closing_month", length = 7, nullable = false)
    private String closingMonth;  // YYYY-MM 형식

    @Column(name = "emp_code", length = 50, nullable = false)
    private String empCode;

    @Column(name = "fp_name", length = 50, nullable = false)
    private String fpName;

    @Column(name = "organization_code", length = 50, nullable = false)
    private String organizationCode;

    @Column(name = "organization_name", length = 100, nullable = false)
    private String organizationName;

    @Column(name = "organization_type", length = 30, nullable = false)
    private String organizationType;

    @Column(name = "career_years", nullable = false)
    private int careerYears;

    @Column(name = "specialty_category", length = 100, nullable = false)
    private String specialtyCategory;  // 전문 보종

    @Column(name = "preferred_customer_age")
    private Integer preferredCustomerAge;  // 선호 고객 연령대

    @Column(name = "preferred_customer_asset_level", length = 30)
    private String preferredCustomerAssetLevel;

    @Column(name = "consultation_channel", length = 20, nullable = false)
    private String consultationChannel;  // 주 상담 채널

    @Column(name = "current_contract_count", nullable = false)
    private int currentContractCount;  // 현재 담당 계약 수

    @Column(name = "retention_rate", nullable = false)
    private BigDecimal retentionRate;  // 유지율 (%)

    @Column(name = "consultation_count", nullable = false)
    private int consultationCount;

    @Column(name = "handover_success_count", nullable = false)
    private int handoverSuccessCount;

    @Column(name = "long_term_maintenance_rate", nullable = false)
    private BigDecimal longTermMaintenanceRate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;
}
