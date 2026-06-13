package com.linker.relia.customer.repository;

import com.linker.relia.customer.domain.CustomerUnderlyingDisease;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerUnderlyingDiseaseRepository extends JpaRepository<CustomerUnderlyingDisease, UUID> {
}
