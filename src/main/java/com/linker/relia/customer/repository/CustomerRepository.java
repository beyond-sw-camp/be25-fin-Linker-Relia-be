package com.linker.relia.customer.repository;

import com.linker.relia.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, CustomerRepositoryCustom {
    boolean existsByIdAndDeletedAtIsNull(UUID customerId);

    Optional<Customer> findByIdAndDeletedAtIsNull(UUID customerId);
}
