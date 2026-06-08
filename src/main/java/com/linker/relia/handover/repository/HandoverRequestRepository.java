package com.linker.relia.handover.repository;

import com.linker.relia.customer.domain.Customer;
import com.linker.relia.handover.domain.HandoverRequest;
import com.linker.relia.handover.domain.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HandoverRequestRepository
        extends JpaRepository<HandoverRequest, String>, HandoverRepositoryCustom {

    // 중복 요청 체크
    // 같은 고객에 MANAGER_PENDING 또는 RETRY 상태 요청이 있으면 true
    boolean existsByCustomerAndRequestStatusIn(Customer customer, List<RequestStatus> statuses);
}