package com.linker.relia.hr.repository;

import com.linker.relia.hr.domain.OrganizationMonthlyClosing;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrganizationMonthlyClosingRepository extends JpaRepository<OrganizationMonthlyClosing, UUID> {
    boolean existsByClosingMonth(String closingMonth);

    long countByClosingMonth(String closingMonth);

    @EntityGraph(attributePaths = "organization")
    List<OrganizationMonthlyClosing> findAllByClosingMonthOrderByOrganizationCodeAsc(String closingMonth);
}
