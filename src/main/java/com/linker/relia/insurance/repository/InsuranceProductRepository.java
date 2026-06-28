package com.linker.relia.insurance.repository;

import com.linker.relia.insurance.domain.InsuranceProduct;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
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

    boolean existsByInsuranceProductCode(String insuranceProductCode);

    @Query("""
            select coalesce(max(cast(substring(ip.insuranceProductCode, 3) as integer)), -1)
            from InsuranceProduct ip
            where ip.insuranceProductCode like 'LP%'
            """)
    long findMaxInsuranceProductCodeSequence();

    List<InsuranceProduct> findAllByInsuranceProductCodeInAndDeletedAtIsNull(Set<String> insuranceProductCodes);

    @EntityGraph(attributePaths = {"insuranceCompany"})
    Optional<InsuranceProduct> findByInsuranceProductCodeAndDeletedAtIsNull(String insuranceProductCode);

    @EntityGraph(attributePaths = {"insuranceCompany"})
    Optional<InsuranceProduct> findByInsuranceProductNameAndDeletedAtIsNull(String insuranceProductName);

    @EntityGraph(attributePaths = {"insuranceCompany"})
    List<InsuranceProduct> findTop10ByInsuranceProductNameContainingAndDeletedAtIsNullOrderByInsuranceProductNameAsc(
            String insuranceProductName
    );

    @Query("""
            select ip
            from InsuranceProduct ip
            join fetch ip.insuranceCompany ic
            join fetch ip.insuranceCategory category
            where ip.id = :insuranceProductId
            """)
    Optional<InsuranceProduct> findManagementInsuranceProductDetail(@Param("insuranceProductId") UUID insuranceProductId);

    @Query(
            value = """
                    select ip
                    from InsuranceProduct ip
                    join fetch ip.insuranceCompany ic
                    join fetch ip.insuranceCategory category
                    where (:insuranceCompanyId is null or ic.id = :insuranceCompanyId)
                      and (:insuranceCategoryId is null or category.id = :insuranceCategoryId)
                      and (:insuranceProductName is null
                           or lower(ip.insuranceProductName) like lower(concat('%', :insuranceProductName, '%')))
                      and (
                           :saleStatus is null
                           or (
                               :saleStatus = 'ON_SALE'
                               and ip.insuranceProductStatus = 'ACTIVE'
                               and ip.deletedAt is null
                               and (ip.insuranceEndDate is null or ip.insuranceEndDate >= :today)
                           )
                           or (
                               :saleStatus = 'SALE_ENDED'
                               and (
                                   ip.insuranceProductStatus = 'INACTIVE'
                                   or ip.deletedAt is not null
                                   or (ip.insuranceEndDate is not null and ip.insuranceEndDate < :today)
                               )
                           )
                      )
                    order by ip.insuranceStartDate desc, ip.id desc
                    """,
            countQuery = """
                    select count(ip)
                    from InsuranceProduct ip
                    join ip.insuranceCompany ic
                    join ip.insuranceCategory category
                    where (:insuranceCompanyId is null or ic.id = :insuranceCompanyId)
                      and (:insuranceCategoryId is null or category.id = :insuranceCategoryId)
                      and (:insuranceProductName is null
                           or lower(ip.insuranceProductName) like lower(concat('%', :insuranceProductName, '%')))
                      and (
                           :saleStatus is null
                           or (
                               :saleStatus = 'ON_SALE'
                               and ip.insuranceProductStatus = 'ACTIVE'
                               and ip.deletedAt is null
                               and (ip.insuranceEndDate is null or ip.insuranceEndDate >= :today)
                           )
                           or (
                               :saleStatus = 'SALE_ENDED'
                               and (
                                   ip.insuranceProductStatus = 'INACTIVE'
                                   or ip.deletedAt is not null
                                   or (ip.insuranceEndDate is not null and ip.insuranceEndDate < :today)
                               )
                           )
                      )
                    """
    )
    Page<InsuranceProduct> searchManagementInsuranceProducts(
            @Param("insuranceCompanyId") UUID insuranceCompanyId,
            @Param("insuranceCategoryId") UUID insuranceCategoryId,
            @Param("insuranceProductName") String insuranceProductName,
            @Param("saleStatus") String saleStatus,
            @Param("today") LocalDate today,
            Pageable pageable
    );
}
