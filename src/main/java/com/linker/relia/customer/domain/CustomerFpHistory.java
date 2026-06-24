package com.linker.relia.customer.domain;

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

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "customer_fp_history")
public class CustomerFpHistory {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @Column(name = "customer_fp_sequence")
    private Integer customerFpSequence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "handover_request_id")
    private UUID handoverRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "before_fp_id")
    private User beforeFp;

    @Column(name = "before_fp_name")
    private String beforeFpName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "after_fp_id")
    private User afterFp;

    @Column(name = "after_fp_name")
    private String afterFpName;

    @Column(name = "changed_reason")
    private String changedReason;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    public static CustomerFpHistory create(
            Customer customer,
            UUID handoverRequestId,
            User beforeFp,
            User afterFp,
            String changedReason
    ) {
        CustomerFpHistory history = new CustomerFpHistory();
        history.id = UUID.randomUUID();
        history.customer = customer;
        history.handoverRequestId = handoverRequestId;
        history.beforeFp = beforeFp;
        history.beforeFpName = beforeFp != null ? beforeFp.getUserName() : null;
        history.afterFp = afterFp;
        history.afterFpName = afterFp != null ? afterFp.getUserName() : null;
        history.changedReason = changedReason;
        history.changedAt = LocalDateTime.now();
        return history;
    }

    public void applyChangeMetadata(User changedBy, int customerFpSequence) {
        this.changedBy = changedBy;
        this.customerFpSequence = customerFpSequence;
    }
}
