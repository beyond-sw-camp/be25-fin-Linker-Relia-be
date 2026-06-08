package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ConsultationRepository extends JpaRepository<Consultation, UUID>, ConsultationRepositoryCustom {

    @Query("""
            select max(c.consultationSequence)
            from Consultation c
            where c.customer.id = :customerId
              and c.deletedAt is null /* 삭제된 상담일지 제외 */
            """)
    Optional<Integer> findMaxSequenceByCustomerId(UUID customerId);
}
