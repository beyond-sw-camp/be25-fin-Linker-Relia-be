package com.linker.relia.hr.repository;

import com.linker.relia.hr.domain.HrMonthlyClosing;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HrMonthlyClosingRepository extends JpaRepository<HrMonthlyClosing, UUID> {
    boolean existsByClosingMonth(String closingMonth);

    long countByClosingMonth(String closingMonth);

    @EntityGraph(attributePaths = {"user", "organization"})
    List<HrMonthlyClosing> findAllByClosingMonthOrderByEmpCodeAsc(String closingMonth);

    @EntityGraph(attributePaths = {"user", "organization"})
    Optional<HrMonthlyClosing> findByClosingMonthAndUser_Id(String closingMonth, UUID userId);
}
