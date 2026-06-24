package com.linker.relia.hr.domain;

import com.linker.relia.organization.domain.Organization;
import com.linker.relia.user.domain.User;
import com.linker.relia.user.domain.UserRole;
import com.linker.relia.user.domain.UserStatus;
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
@Table(name = "hr_monthly_closing")
public class HrMonthlyClosing {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @Column(name = "closing_month")
    private String closingMonth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "emp_code")
    private String empCode;

    @Column(name = "user_name")
    private String userName;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role")
    private UserRole userRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status")
    private UserStatus userStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    public static HrMonthlyClosing snapshot(String closingMonth, User user, LocalDateTime closedAt) {
        return HrMonthlyClosing.builder()
                .id(UUID.randomUUID())
                .closingMonth(closingMonth)
                .user(user)
                .empCode(user.getEmpCode())
                .userName(user.getUserName())
                .userRole(user.getUserRole())
                .userStatus(user.getUserStatus())
                .organization(user.getOrganization())
                .closedAt(closedAt)
                .build();
    }
}
