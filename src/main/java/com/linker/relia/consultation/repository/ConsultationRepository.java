package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.Consultation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ConsultationRepository extends JpaRepository<Consultation, UUID>, ConsultationRepositoryCustom {

    /**
     * Retrieves the highest consultation sequence number for the given customer excluding soft-deleted records.
     *
     * @param customerId the UUID of the customer to search sequences for
     * @return an Optional containing the maximum consultation sequence for the customer, or empty if none exist
     */
    @Query("""
            select max(c.consultationSequence)
            from Consultation c
            where c.customer.id = :customerId
              and c.deletedAt is null /* 삭제된 상담일지 제외 */
            """)
    Optional<Integer> findMaxSequenceByCustomerId(UUID customerId);

    /**
 * Retrieve consultations that are not soft-deleted using the provided pagination and sorting.
 *
 * @param pageable pagination and sorting information
 * @return a page of Consultation entities with `deletedAt` equal to null
 */
Page<Consultation> findAllByDeletedAtIsNull(Pageable pageable);
}
