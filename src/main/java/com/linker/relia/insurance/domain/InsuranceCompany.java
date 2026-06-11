package com.linker.relia.insurance.domain;

import com.linker.relia.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "insurance_companies")
public class InsuranceCompany extends BaseEntity {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @Column(name = "insurance_company_name")
    private String insuranceCompanyName;

    @Column(name = "insurance_company_status")
    private String insuranceCompanyStatus;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
