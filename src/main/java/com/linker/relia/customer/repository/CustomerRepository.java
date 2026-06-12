package com.linker.relia.customer.repository;

import com.linker.relia.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, CustomerRepositoryCustom {
    @Query(value = """
            select coalesce(max(cast(substring(customer_code, 4) as unsigned)), 0)
            from customers
            where customer_code regexp '^CUS[0-9]+$'
            """, nativeQuery = true)
    long findMaxCustomerCodeSequence();

    boolean existsByIdAndDeletedAtIsNull(UUID customerId);

    Optional<Customer> findByIdAndDeletedAtIsNull(UUID customerId);

    @Query(value = """
            select count(*)
            from customers
            where deleted_at is null
              and replace(replace(customer_phone, '-', ''), ' ', '') = :normalizedPhone
            """, nativeQuery = true)
    long countActiveCustomerByNormalizedPhone(@Param("normalizedPhone") String normalizedPhone);
}
