package com.linker.relia.insurance.repository;

import com.linker.relia.insurance.domain.InsuranceProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, UUID> {
    @Query("""
            select ip
            from InsuranceProduct ip
            join fetch ip.insuranceCompany ic
            join fetch ip.insuranceCategory category
            where ip.insuranceProductStatus = :activeStatus
              and ip.deletedAt is null
              and ic.insuranceCompanyStatus = :activeStatus
              and ic.deletedAt is null
              and category.insuranceCategoryStatus = :activeStatus
              and category.deletedAt is null
              and (:insuranceCompanyCode is null or ic.insuranceCompanyCode = :insuranceCompanyCode)
              and (:insuranceCategoryCode is null or category.insuranceCategoryCode = :insuranceCategoryCode)
            order by ip.insuranceProductName asc
            """)
    List<InsuranceProduct> searchActiveInsuranceProducts(
            @Param("insuranceCompanyCode") String insuranceCompanyCode,
            @Param("insuranceCategoryCode") String insuranceCategoryCode,
            @Param("activeStatus") String activeStatus
    );

    Optional<InsuranceProduct> findByIdAndInsuranceProductStatusAndDeletedAtIsNull(
            UUID insuranceProductId,
            String insuranceProductStatus
    );

    Optional<InsuranceProduct> findByInsuranceProductCodeAndDeletedAtIsNull(String insuranceProductCode);
}
