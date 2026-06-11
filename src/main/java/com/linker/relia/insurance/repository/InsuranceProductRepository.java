package com.linker.relia.insurance.repository;

import com.linker.relia.insurance.domain.InsuranceProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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
              and (:insuranceCompanyId is null or ic.id = :insuranceCompanyId)
              and (:insuranceCategoryId is null or category.id = :insuranceCategoryId)
            order by ip.insuranceProductName asc
            """)
    List<InsuranceProduct> searchActiveInsuranceProducts(
            @Param("insuranceCompanyId") UUID insuranceCompanyId,
            @Param("insuranceCategoryId") UUID insuranceCategoryId,
            @Param("activeStatus") String activeStatus
    );
}
