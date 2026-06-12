package com.linker.relia.handover.repository;

import com.linker.relia.consultation.domain.ConsultationChannel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class HandoverRecommendationQueryRepository { //추천 계산에 필요한 고객 보종/상담채널 조회용 query repository

    @PersistenceContext
    private EntityManager entityManager;

    public List<String> findCustomerCategories(UUID customerId) {
        return entityManager.createQuery("""
                SELECT ip.insuranceCategory.insuranceCategoryName
                FROM Contract ct
                JOIN ct.insuranceProduct ip
                WHERE ct.customer.id = :customerId
                AND ct.contractStatus = 'MAINTENANCE'
                AND ct.deletedAt IS NULL
                GROUP BY ip.insuranceCategory.id
                """, String.class)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    public String findMainChannel(UUID customerId) {
        List<ConsultationChannel> result = entityManager.createQuery("""
            SELECT c.consultationChannel
            FROM Consultation c
            WHERE c.customer.id = :customerId
            AND c.deletedAt IS NULL
            GROUP BY c.consultationChannel
            ORDER BY COUNT(c.id) DESC
            """, ConsultationChannel.class)
                .setParameter("customerId", customerId)
                .setMaxResults(1)
                .getResultList();

        return result.isEmpty() ? null : result.get(0).name();
    }

    public List<String> findCustomerHistoryFpEmpCodes(UUID customerId) {
        return entityManager.createQuery("""
            SELECT DISTINCT u.empCode
            FROM User u
            WHERE u.id IN (
                SELECT h.beforeFpId
                FROM CustomerFpHistory h
                WHERE h.customer.id = :customerId
                  AND h.beforeFpId IS NOT NULL
            )
            OR u.id IN (
                SELECT h.afterFpId
                FROM CustomerFpHistory h
                WHERE h.customer.id = :customerId
                  AND h.afterFpId IS NOT NULL
            )
            """, String.class)
                .setParameter("customerId", customerId)
                .getResultList();
    }
}
