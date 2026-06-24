package com.linker.relia.handover.repository;

import com.linker.relia.customer.domain.Customer;
import com.linker.relia.handover.domain.HandoverRequest;
import com.linker.relia.handover.domain.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HandoverRequestRepository
        extends JpaRepository<HandoverRequest, UUID>, HandoverRequestSearchRepository { // 요청 CRUD + 요청 검색 fragment

    // 중복 요청 체크
    // 같은 고객에 MANAGER_PENDING 또는 RETRY 상태 요청이 있으면 true
    boolean existsByCustomerAndRequestStatusIn(Customer customer, List<RequestStatus> statuses);
}
