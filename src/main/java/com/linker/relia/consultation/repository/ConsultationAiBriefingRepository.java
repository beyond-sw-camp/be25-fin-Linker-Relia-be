package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.ConsultationAiBriefing;
import com.linker.relia.customer.dto.CustomerAiBriefingResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ConsultationAiBriefingRepository extends JpaRepository<ConsultationAiBriefing, UUID> {
    @Query("""
            select new com.linker.relia.customer.dto.CustomerAiBriefingResponse(
                b.id,
                b.briefingContent,
                b.createdAt
            )
            from ConsultationAiBriefing b
            where b.customer.id = :customerId
              and b.deletedAt is null
              and b.updateSequence = (
                  select max(b2.updateSequence)
                  from ConsultationAiBriefing b2
                  where b2.customer.id = :customerId
                    and b2.deletedAt is null
              )
              and b.createdAt = (
                  select max(b3.createdAt)
                  from ConsultationAiBriefing b3
                  where b3.customer.id = :customerId
                    and b3.deletedAt is null
                    and b3.updateSequence = b.updateSequence
              )
            """)
    Optional<CustomerAiBriefingResponse> findOwnCustomerLatestAiBriefing(@Param("customerId") UUID customerId);

    @Query("""
            select coalesce(max(b.updateSequence), 0)
            from ConsultationAiBriefing b
            where b.customer.id = :customerId
              and b.deletedAt is null
            """)
    Integer findMaxUpdateSequenceByCustomerId(@Param("customerId") UUID customerId);


}
