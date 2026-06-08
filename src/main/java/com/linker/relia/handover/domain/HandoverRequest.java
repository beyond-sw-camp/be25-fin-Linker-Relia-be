package com.linker.relia.handover.domain;

import com.linker.relia.customer.domain.Customer;
import com.linker.relia.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "handover_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HandoverRequest {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    // @ManyToOne: 여러 요청이 하나의 고객에 연결 (N:1)
    // fetch = LAZY: 실제로 customer 객체를 쓸 때만 SELECT 쿼리 날림
    @JoinColumn(name = "customer_id", nullable = false) // @JoinColumn: 실제 FK 컬럼명 지정
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 36, nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 36, nullable = false)
    private String updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 36)
    private String deletedBy;

    // 정적 팩토리 메서드
    public static HandoverRequest create(Customer customer, User currentFp,
                                         RequestType requestType, String createdBy) {
        HandoverRequest request = new HandoverRequest();
        request.id = UUID.randomUUID().toString();
        request.customer = customer;
        request.currentFp = currentFp;
        request.requestType = requestType;
        request.requestStatus = RequestStatus.MANAGER_PENDING;
        request.createdAt = LocalDateTime.now();
        request.createdBy = createdBy;
        request.updatedAt = LocalDateTime.now();
        request.updatedBy = createdBy;
        return request;
    }

    // 비즈니스 메서드
    public void complete(String updatedBy) {
        this.requestStatus = RequestStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }

    public void retry(String updatedBy) {
        this.requestStatus = RequestStatus.RETRY;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }

    public void softDelete(String deletedBy) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = deletedBy;
    }
}