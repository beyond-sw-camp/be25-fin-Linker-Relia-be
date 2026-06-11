package com.linker.relia.handover.repository;

import com.linker.relia.consultation.domain.ConsultationChannel;
import com.linker.relia.user.domain.FpMonthlyInfo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class HandoverDetailQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    // 보유 계약 요약 "실손 2건 · 종신 1건"
    public String findContractSummary(UUID customerId) {
        List<Object[]> results = entityManager.createQuery("""
            SELECT ic.insuranceCategoryName, COUNT(ct)
            FROM Contract ct
            JOIN ct.insuranceProduct ip
            JOIN ip.insuranceCategory ic
            WHERE ct.customer.id = :customerId
            AND ct.contractStatus = 'MAINTENANCE'
            AND ct.deletedAt IS NULL
            GROUP BY ic.insuranceCategoryName
            """, Object[].class)
                .setParameter("customerId", customerId)
                .getResultList();

        return results.stream()
                .map(r -> r[0] + " " + r[1] + "건")
                .collect(Collectors.joining(" · "));
    }

    // 월 보험료 합산
    public BigDecimal findMonthlyPremium(UUID customerId) {
        List<BigDecimal> result = entityManager.createQuery("""
            SELECT COALESCE(SUM(ct.monthlyPremium), 0)
            FROM Contract ct
            WHERE ct.customer.id = :customerId
            AND ct.contractStatus = 'MAINTENANCE'
            AND ct.deletedAt IS NULL
            """, BigDecimal.class)
                .setParameter("customerId", customerId)
                .getResultList();
        return result.isEmpty() ? BigDecimal.ZERO : result.get(0);
    }

    // 최근 상담일
    public LocalDateTime findLastConsultedAt(UUID customerId) {
        List<LocalDateTime> result = entityManager.createQuery("""
            SELECT MAX(c.consultedAt)
            FROM Consultation c
            WHERE c.customer.id = :customerId
            AND c.deletedAt IS NULL
            """, LocalDateTime.class)
                .setParameter("customerId", customerId)
                .getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    // 주 상담채널
    public Optional<ConsultationChannel> findMainChannel(UUID customerId) {
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
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    // 이전 인수인계 이력 (가장 최근 1건)
    public Optional<Object[]> findLatestHistory(UUID customerId) {
        List<Object[]> result = entityManager.createQuery("""
            SELECT h.beforeFpName, h.changedAt
            FROM CustomerFpHistory h
            WHERE h.customer.id = :customerId
            ORDER BY h.changedAt DESC
            """, Object[].class)
                .setParameter("customerId", customerId)
                .setMaxResults(1)
                .getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    // 반려된 설계사명 (재추천 여부 확인)
    public Optional<String> findRejectedFpName(UUID handoverRequestId) {
        List<String> result = entityManager.createQuery("""
            SELECT r.recommendedFpName
            FROM HandoverRecommendation r
            WHERE r.handoverRequest.id = :handoverRequestId
            AND r.approvalStatus = 'REJECTED'
            ORDER BY r.createdAt DESC
            """, String.class)
                .setParameter("handoverRequestId", handoverRequestId)
                .setMaxResults(1)
                .getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public Optional<FpMonthlyInfo> findLatestFpMonthlyInfo(String empCode) {
        List<FpMonthlyInfo> result = entityManager.createQuery("""
        SELECT f FROM FpMonthlyInfo f
        WHERE f.empCode = :empCode
        ORDER BY f.closingMonth DESC
        """, FpMonthlyInfo.class)
                .setParameter("empCode", empCode)
                .setMaxResults(1)
                .getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }
}
