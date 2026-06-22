package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.ConsultationAiBriefing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ConsultationAiBriefingRepository extends JpaRepository<ConsultationAiBriefing, UUID> {
    Optional<ConsultationAiBriefing> findFirstByCustomer_IdAndDeletedAtIsNullOrderByUpdateSequenceDescCreatedAtDesc(
            UUID customerId
    );

    @Query("""
            select coalesce(max(b.updateSequence), 0)
            from ConsultationAiBriefing b
            where b.customer.id = :customerId
              and b.deletedAt is null
            """)
    Integer findMaxUpdateSequenceByCustomerId(@Param("customerId") UUID customerId);


}
