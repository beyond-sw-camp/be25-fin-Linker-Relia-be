package com.linker.relia.handover.repository;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.consultation.domain.ConsultationChannel;
import com.linker.relia.handover.domain.RequestStatus;
import com.linker.relia.handover.dto.response.HandoverAssignableFpResponse;
import com.linker.relia.handover.dto.response.HandoverBranchSummaryResponse;
import com.linker.relia.handover.dto.response.HandoverMonthlyTrendResponse;
import com.linker.relia.handover.dto.response.HandoverSummaryResponse;
import com.linker.relia.user.domain.FpMonthlyInfo;
import com.linker.relia.user.domain.UserRole;
import com.linker.relia.user.domain.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.Flyway;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    // 요청 요약 카드
    public HandoverSummaryResponse findSummary(AccessScope accessScope, String organizationCode) {
        String jpql = """
            SELECT new com.linker.relia.handover.dto.response.HandoverSummaryResponse(
                COALESCE(SUM(CASE WHEN h.requestStatus = :pendingStatus THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN h.requestStatus = :completedStatus
                    AND YEAR(h.updatedAt) = YEAR(CURRENT_DATE)
                    AND MONTH(h.updatedAt) = MONTH(CURRENT_DATE) THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN YEAR(h.createdAt) = YEAR(CURRENT_DATE)
                    AND MONTH(h.createdAt) = MONTH(CURRENT_DATE) THEN 1 ELSE 0 END), 0)
            )
            FROM HandoverRequest h
            LEFT JOIN h.currentFp cfp
            LEFT JOIN cfp.organization org
            WHERE h.deletedAt IS NULL
            AND (:organizationCode IS NULL OR org.organizationCode = :organizationCode)
            """;

        if (accessScope.isBranchScope()) {
            jpql += " AND org.id = :organizationId";
        }

        TypedQuery<HandoverSummaryResponse> query =
                entityManager.createQuery(jpql, HandoverSummaryResponse.class);

        query.setParameter("organizationCode", organizationCode);
        query.setParameter("pendingStatus", RequestStatus.MANAGER_PENDING);
        query.setParameter("completedStatus", RequestStatus.COMPLETED);

        if (accessScope.isBranchScope()) {
            query.setParameter("organizationId", accessScope.organizationId());
        }

        return query.getSingleResult();
    }

    // 지정할 때 설계사 목록
    public Page<HandoverAssignableFpResponse> findAssignableFps(String organizationCode, Pageable pageable) {
        List<HandoverAssignableFpResponse> content = entityManager.createQuery("""
            SELECT new com.linker.relia.handover.dto.response.HandoverAssignableFpResponse(
                u.id,
                COALESCE(fmi.fpName, u.userName),
                fmi.careerYears,
                fmi.specialtyCategory,
                fmi.preferredCustomerAge,
                fmi.consultationChannel,
                (SELECT COUNT(c) FROM Customer c WHERE c.customerFp.id = u.id AND c.deletedAt IS NULL),
                (SELECT COUNT(ct) FROM Contract ct WHERE ct.fp.id = u.id AND ct.contractStatus = 'MAINTENANCE' AND ct.deletedAt IS NULL),
                fmi.retentionRate
            )
            FROM User u
            LEFT JOIN FpMonthlyInfo fmi
                ON fmi.empCode = u.empCode
                AND fmi.organizationCode = :organizationCode
                AND fmi.closingMonth = (
                SELECT MAX(fmi2.closingMonth)
                FROM FpMonthlyInfo fmi2
                WHERE fmi2.empCode = fmi.empCode
                AND fmi2.organizationCode = :organizationCode
            )
            WHERE u.organization.organizationCode = :organizationCode
            AND u.userRole = :userRole
            AND u.userStatus = :userStatus
            AND u.deletedAt IS NULL
            ORDER BY fmi.retentionRate DESC NULLS LAST, u.userName ASC
            """, HandoverAssignableFpResponse.class)
                .setParameter("organizationCode", organizationCode)
                .setParameter("userRole", UserRole.FP)
                .setParameter("userStatus", UserStatus.ACTIVE)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        Long total = entityManager.createQuery("""
            SELECT COUNT(u)
            FROM User u
            WHERE u.organization.organizationCode = :organizationCode
            AND u.userRole = :userRole
            AND u.userStatus = :userStatus
            AND u.deletedAt IS NULL
            """, Long.class)
                .setParameter("organizationCode", organizationCode)
                .setParameter("userRole", UserRole.FP)
                .setParameter("userStatus", UserStatus.ACTIVE)
                .getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    // 월별 인수인계 요청 건수 추이.
    // GROUP BY + DATE_FORMAT을 JPQL FUNCTION()으로 쓰면 Hibernate 버전에 따라 동작이 갈릴 수 있어서 네이티브 쿼리로 작성함.

    @SuppressWarnings("unchecked")
    public List<HandoverMonthlyTrendResponse> findMonthlyTrend(
            AccessScope accessScope,
            String organizationCode,
            LocalDate fromDate,
            LocalDate toDate) {
        StringBuilder sql = new StringBuilder("""
            SELECT DATE_FORMAT(h.created_at, '%Y-%m') AS ym, COUNT(*) AS request_count
            FROM handover_requests h
            LEFT JOIN users cfp ON cfp.id = h.current_fp_id
            LEFT JOIN organizations org ON org.id = cfp.organization_id
            WHERE h.deleted_at IS NULL
              AND h.created_at >= :fromDate
              AND h.created_at < :toDate
            """);

        if (organizationCode != null) {
            sql.append(" AND org.organization_code = :organizationCode");
        }

        if (accessScope.isBranchScope()) {
            sql.append(" AND org.id = :organizationId");
        }
        sql.append(" GROUP BY DATE_FORMAT(h.created_at, '%Y-%m') ORDER BY ym");

        Query query = entityManager.createNativeQuery(sql.toString());
        if (organizationCode != null) {
            query.setParameter("organizationCode", organizationCode);
        }
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        if (accessScope.isBranchScope()) {
            query.setParameter("organizationId", accessScope.organizationId().toString());
        }

        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new HandoverMonthlyTrendResponse((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    // 지점별 현황
    @SuppressWarnings("unchecked")
    public List<HandoverBranchSummaryResponse> findBranchSummary(
            AccessScope accessScope,
            LocalDate fromDate,
            LocalDate toDate) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                org.id AS organization_id,
                org.organization_name AS organization_name,
                COUNT(h.id) AS total_count,
                COALESCE(SUM(CASE WHEN h.request_status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completed_count,
                CASE WHEN COUNT(h.id) = 0 THEN 0.0
                     ELSE (COALESCE(SUM(CASE WHEN h.request_status = 'COMPLETED' THEN 1 ELSE 0 END), 0) * 100.0) / COUNT(h.id)
                END AS completion_rate,
                COALESCE(SUM(CASE WHEN h.request_status = 'MANAGER_PENDING' THEN 1 ELSE 0 END), 0) AS pending_count,
                CASE WHEN COUNT(h.id) = 0 THEN 0.0
                     ELSE (COALESCE(SUM(CASE WHEN h.request_status = 'MANAGER_PENDING' THEN 1 ELSE 0 END), 0) * 100.0) / COUNT(h.id)
                END AS pending_rate
            FROM organizations org
            LEFT JOIN users cfp
                ON cfp.organization_id = org.id
            LEFT JOIN handover_requests h
                ON h.current_fp_id = cfp.id
                AND h.deleted_at IS NULL
                AND h.created_at >= :fromDate
                AND h.created_at < :toDate
            WHERE org.deleted_at IS NULL
              AND org.organization_code LIKE 'BR%'
            """);

        if (accessScope.isBranchScope()) {
            sql.append(" AND org.id = :organizationId");
        }
        sql.append(" GROUP BY org.id, org.organization_name ORDER BY total_count DESC, org.organization_name ASC");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        if (accessScope.isBranchScope()) {
            query.setParameter("organizationId", accessScope.organizationId().toString());
        }

        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new HandoverBranchSummaryResponse(
                        toUuid(row[0]),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).doubleValue(),
                        ((Number) row[5]).longValue(),
                        ((Number) row[6]).doubleValue()
                ))
                .map(HandoverBranchSummaryResponse::rounded)
                .toList();
    }

    private UUID toUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }
}
