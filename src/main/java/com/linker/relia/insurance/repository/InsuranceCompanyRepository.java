package com.linker.relia.insurance.repository;

import com.linker.relia.insurance.domain.InsuranceCompany;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InsuranceCompanyRepository extends JpaRepository<InsuranceCompany, UUID>, InsuranceCompanyRepositoryCustom {
    List<InsuranceCompany> findAllByInsuranceCompanyStatusAndDeletedAtIsNullOrderByInsuranceCompanyNameAsc(
            String insuranceCompanyStatus
    );

    @Query(
            value = """
                    select ic
                    from InsuranceCompany ic
                    where (:insuranceCompanyName is null
                           or lower(ic.insuranceCompanyName) like lower(concat('%', :insuranceCompanyName, '%')))
                    order by ic.createdAt desc, ic.id desc
                    """,
            countQuery = """
                    select count(ic)
                    from InsuranceCompany ic
                    where (:insuranceCompanyName is null
                           or lower(ic.insuranceCompanyName) like lower(concat('%', :insuranceCompanyName, '%')))
                    """
    )
    Page<InsuranceCompany> searchManagementInsuranceCompanies(
            @Param("insuranceCompanyName") String insuranceCompanyName,
            Pageable pageable
    );
}
