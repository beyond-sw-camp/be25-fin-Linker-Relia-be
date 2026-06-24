package com.linker.relia.handover.repository;

import com.linker.relia.handover.dto.response.HandoverReceivedItemResponse;
import com.linker.relia.handover.dto.response.HandoverReceivedSummaryResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class HandoverReceivedQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Page<HandoverReceivedItemResponse> findReceivedHandovers(UUID fpId, Pageable pageable) {

        List<HandoverReceivedItemResponse> content = entityManager.createQuery("""
                SELECT new com.linker.relia.handover.dto.response.HandoverReceivedItemResponse(
                    cfh.handoverRequestId,
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

    // 받은 인수인계 요약 카드
    public HandoverReceivedSummaryResponse findReceivedSummary(UUID fpId) {

        Long thisMonthCount = entityManager.createQuery("""
            SELECT COUNT(cfh) FROM CustomerFpHistory cfh
            WHERE cfh.afterFp.id = :fpId
            AND YEAR(cfh.changedAt) = YEAR(CURRENT_DATE)
            AND MONTH(cfh.changedAt) = MONTH(CURRENT_DATE)
            """, Long.class)
                .setParameter("fpId", fpId)
                .getSingleResult();

        Long totalCount = entityManager.createQuery("""
            SELECT COUNT(cfh) FROM CustomerFpHistory cfh
            WHERE cfh.afterFp.id = :fpId
            """, Long.class)
                .setParameter("fpId", fpId)
                .getSingleResult();

        Long maintainedCount = entityManager.createQuery("""
            SELECT COUNT(DISTINCT c.id)
            FROM CustomerFpHistory cfh
            JOIN cfh.customer c
            JOIN Contract ct ON ct.customer.id = c.id
            WHERE cfh.afterFp.id = :fpId
            AND ct.contractStatus = 'MAINTENANCE'
            AND ct.deletedAt IS NULL
            """, Long.class)
                .setParameter("fpId", fpId)
                .getSingleResult();

        double successRate = totalCount == 0 ? 0.0
                : Math.round((double) maintainedCount / totalCount * 1000.0) / 10.0;

        return new HandoverReceivedSummaryResponse(thisMonthCount, totalCount, successRate);
    }
}
