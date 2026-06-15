package com.linker.relia.handover.repository;

import com.linker.relia.handover.dto.response.HandoverReceivedItemResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class HandoverReceivedQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Page<HandoverReceivedItemResponse> findReceivedHandovers(UUID fpId, Pageable pageable) {

        List<HandoverReceivedItemResponse> content = entityManager.createQuery("""
                SELECT new com.linker.relia.handover.dto.response.HandoverReceivedItemResponse(
                    c.id,
                    c.customerName,
                    c.customerGrade,
                    cfh.beforeFpName,
                    cfh.changedReason,
                    cfh.changedAt
                )
                FROM CustomerFpHistory cfh
                JOIN cfh.customer c
                JOIN cfh.afterFp afterFp
                WHERE afterFp.id = :fpId
                AND cfh.customerFpSequence = (
                    SELECT MAX(cfh2.customerFpSequence)
                    FROM CustomerFpHistory cfh2
                    WHERE cfh2.customer.id = c.id
                )
                ORDER BY cfh.changedAt DESC
                """, HandoverReceivedItemResponse.class)
                .setParameter("fpId", fpId)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        Long total = entityManager.createQuery("""
                SELECT COUNT(cfh)
                FROM CustomerFpHistory cfh
                JOIN cfh.customer c
                JOIN cfh.afterFp afterFp
                WHERE afterFp.id = :fpId
                AND cfh.customerFpSequence = (
                    SELECT MAX(cfh2.customerFpSequence)
                    FROM CustomerFpHistory cfh2
                    WHERE cfh2.customer.id = c.id
                )
                """, Long.class)
                .setParameter("fpId", fpId)
                .getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }
}
