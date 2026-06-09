package com.linker.relia.customer.domain;

import com.linker.relia.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "customers")
public class Customer {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @Column(name = "customer_code")
    private String customerCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_fp_id")
    private User customerFp;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_status")
    private CustomerStatus customerStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_grade")
    private CustomerGrade customerGrade;

    @Column(name = "interest_yn")
    private boolean interestYn;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_reason")
    private InterestReason interestReason;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_gender")
    private String customerGender;

    @Column(name = "customer_birth_date")
    private LocalDate customerBirthDate;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_zipcode")
    private String customerZipcode;

    @Column(name = "customer_address_road")
    private String customerAddressRoad;

    @Column(name = "customer_address_detail")
    private String customerAddressDetail;

    @Column(name = "customer_job")
    private String customerJob;

    @Column(name = "customer_company_name")
    private String customerCompanyName;

    @Column(name = "customer_annual_income")
    private BigDecimal customerAnnualIncome;

    @Column(name = "customer_asset_size")
    private BigDecimal customerAssetSize;

    @Column(name = "customer_debt_status")
    private String customerDebtStatus;

    @Column(name = "customer_is_smoker")
    private boolean customerIsSmoker;

    @Column(name = "customer_is_drinker")
    private boolean customerIsDrinker;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_marital_status")
    private CustomerMaritalStatus customerMaritalStatus;

    @Column(name = "customer_dependents_count")
    private int customerDependentsCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID deletedBy;
}
