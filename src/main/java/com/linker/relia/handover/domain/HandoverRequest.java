package com.linker.relia.handover.domain;

import com.linker.relia.common.domain.BaseEntity;
import com.linker.relia.customer.domain.Customer;
import com.linker.relia.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "handover_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HandoverRequest extends BaseEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_fp_id")
    private User currentFp;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", length = 30, nullable = false)
    private RequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", length = 30, nullable = false)
    private RequestStatus requestStatus;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "deleted_by")
    private UUID deletedBy;

    public static HandoverRequest create(Customer customer,
                                         RequestType requestType) {
        HandoverRequest request = new HandoverRequest();
        request.id = UUID.randomUUID();
        request.customer = customer;
        request.currentFp = customer.getCustomerFp();
        request.requestType = requestType;
        request.requestStatus = RequestStatus.MANAGER_PENDING;
        return request;
    }

    public void complete() {
        this.requestStatus = RequestStatus.COMPLETED;
    }

    public void retry() {
        this.requestStatus = RequestStatus.RETRY;
    }

    public void softDelete(UUID deletedBy) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }
}