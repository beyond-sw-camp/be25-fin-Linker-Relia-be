package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.Consultation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    Page<Consultation> findAllByDeletedAtIsNull(Pageable pageable);

    @Query("""
            select c
            from Consultation c
            where c.deletedAt is null
              and c.fp.organization.id = :organizationId
            """)
    Page<Consultation> findAllByAuthorOrganizationId(
            @Param("organizationId") UUID organizationId,
            Pageable pageable
    );

    @Query("""
            select c
            from Consultation c
            where c.deletedAt is null
              and (c.fp.id = :userId or c.customer.customerFp.id = :userId)
            """)
    Page<Consultation> findAllAccessibleByFpId(
            @Param("userId") UUID userId,
            Pageable pageable
    );

    Optional<Consultation> findByIdAndDeletedAtIsNull(UUID consultationId);

    boolean existsByCustomer_IdAndCreatedAtAfterAndDeletedAtIsNull(
            UUID customerId,
            LocalDateTime createdAt
    );
}
