package com.linker.relia.insurance.repository;

import com.linker.relia.insurance.domain.InsuranceProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, UUID> {
}