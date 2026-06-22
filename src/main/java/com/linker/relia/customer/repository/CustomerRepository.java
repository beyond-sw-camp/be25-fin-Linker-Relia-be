package com.linker.relia.customer.repository;

import com.linker.relia.customer.domain.Customer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, CustomerRepositoryCustom {
    @Query(value = """
            select next value for customer_code_seq
            """, nativeQuery = true)
    long getNextCustomerCodeSequence();

    boolean existsByIdAndDeletedAtIsNull(UUID customerId);

    Optional<Customer> findByIdAndDeletedAtIsNull(UUID customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.id = :customerId")
    Optional<Customer> findByIdForUpdate(@Param("customerId") UUID customerId);
}
