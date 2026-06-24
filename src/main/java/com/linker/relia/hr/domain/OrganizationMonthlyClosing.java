package com.linker.relia.hr.domain;

import com.linker.relia.organization.domain.Organization;
import com.linker.relia.organization.domain.OrganizationStatus;
import com.linker.relia.organization.domain.OrganizationType;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "organization_monthly_closing")
public class OrganizationMonthlyClosing {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @Column(name = "closing_month")
    private String closingMonth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "organization_code")
    private String organizationCode;

    @Column(name = "organization_name")
    private String organizationName;

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type")
    private OrganizationType organizationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_status")
    private OrganizationStatus organizationStatus;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    public static OrganizationMonthlyClosing snapshot(String closingMonth,
                                                      Organization organization,
                                                      LocalDateTime closedAt) {
        return OrganizationMonthlyClosing.builder()
                .id(UUID.randomUUID())
                .closingMonth(closingMonth)
                .organization(organization)
                .organizationCode(organization.getOrganizationCode())
                .organizationName(organization.getOrganizationName())
                .organizationType(organization.getOrganizationType())
                .organizationStatus(organization.getOrganizationStatus())
                .closedAt(closedAt)
                .build();
    }
}
