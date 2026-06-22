package com.linker.relia.insurance.domain;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "insurance_products")
public class InsuranceProduct {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @Column(name = "insurance_product_code")
    private String insuranceProductCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_company_id")
    private InsuranceCompany insuranceCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_category_id")
    private InsuranceCategory insuranceCategory;

    @Column(name = "insurance_product_name")
    private String insuranceProductName;

    @Column(name = "insurance_product_status")
    private String insuranceProductStatus;

    @Column(name = "insurance_start_date")
    private LocalDate insuranceStartDate;

    @Column(name = "insurance_end_date")
    private LocalDate insuranceEndDate;

    @Column(name = "coverage_period_type")
    private String coveragePeriodType;

    @Column(name = "coverage_period_years")
    private Integer coveragePeriodYears;

    @Column(name = "coverage_age_limit")
    private Integer coverageAgeLimit;

    @Column(name = "is_lifetime_coverage")
    private Boolean isLifetimeCoverage;

    @Column(name = "is_renewable")
    private Boolean isRenewable;

    @Column(name = "renewal_cycle")
    private Integer renewalCycle;

    @Column(name = "product_description")
    private String productDescription;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void updateManagementInfo(String insuranceProductStatus, String productDescription) {
        this.insuranceProductStatus = insuranceProductStatus;
        this.productDescription = productDescription;

        if ("INACTIVE".equals(insuranceProductStatus)) {
            this.deletedAt = LocalDateTime.now();
            return;
        }

        this.deletedAt = null;
    }
}
