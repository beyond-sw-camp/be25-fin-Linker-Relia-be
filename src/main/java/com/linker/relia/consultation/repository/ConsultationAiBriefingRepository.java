package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.ConsultationAiBriefing;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ConsultationAiBriefingRepository extends JpaRepository<ConsultationAiBriefing, UUID> {
    @Query("""
            select b
            from ConsultationAiBriefing b
            where b.customer.id = :customerId
              and b.deletedAt is null
              and b.updateSequence = (
                  select max(b2.updateSequence)
                  from ConsultationAiBriefing b2
                  where b2.customer.id = :customerId
                    and b2.deletedAt is null
              )
            """)
    Optional<ConsultationAiBriefing> findOwnCustomerLatestAiBriefing(@Param("customerId") UUID customerId);
}
