package com.linker.relia.customer.repository;

import com.linker.relia.customer.domain.CustomerFpHistory;
import com.linker.relia.customer.dto.CustomerFpHistoryItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CustomerFpHistoryRepository extends JpaRepository<CustomerFpHistory, UUID> {
    @Query(
            value = """
                    select new com.linker.relia.customer.dto.CustomerFpHistoryItemResponse(
                        h.id,
                        h.customerFpSequence,
                        h.changedAt,
                        h.beforeFpName,
                        h.afterFpName,
                        h.changedReason
                    )
                    from CustomerFpHistory h
                    where h.customer.id = :customerId
                    order by h.changedAt desc
                    """,
            countQuery = """
                    select count(h)
                    from CustomerFpHistory h
                    where h.customer.id = :customerId
                    """
    )
    Page<CustomerFpHistoryItemResponse> findCustomerFpHistories(@Param("customerId") UUID customerId, Pageable pageable);
}
