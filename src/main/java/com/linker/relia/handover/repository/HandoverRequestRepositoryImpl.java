package com.linker.relia.handover.repository;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.handover.domain.RequestStatus;
import com.linker.relia.handover.domain.RequestType;
import com.linker.relia.handover.dto.response.HandoverListItemResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class HandoverRequestRepositoryImpl implements HandoverRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<HandoverListItemResponse> searchHandovers(
            AccessScope accessScope,
            RequestStatus status,
            RequestType requestType,
            String customerName,
            Pageable pageable
    ) {
        // 목록 조회 쿼리
        // LEFT JOIN h.currentFp fp → 해촉 설계사는 null이라 LEFT JOIN 사용
        String contentJpql = """
                SELECT new com.linker.relia.handover.dto.HandoverListItemResponse(
                    h.id,
                    c.customerName,
                    c.customerGrade,
                    fp.userName,
                    h.requestType,
                    h.requestStatus,
                    h.createdAt
                )
                FROM HandoverRequest h
                JOIN h.customer c
                LEFT JOIN h.currentFp fp
                JOIN c.customerFp cfp
                JOIN cfp.organization org
                """ + buildWhereClause(accessScope) + """
                ORDER BY h.createdAt DESC
                """;

        TypedQuery<HandoverListItemResponse> contentQuery =
                entityManager.createQuery(contentJpql, HandoverListItemResponse.class);

        bindParams(contentQuery, accessScope, status, requestType, customerName);
        contentQuery.setFirstResult((int) pageable.getOffset());
        contentQuery.setMaxResults(pageable.getPageSize());

        // 전체 건수 조회 (페이지네이션용)
        String countJpql = """
                SELECT COUNT(h)
                FROM HandoverRequest h
                JOIN h.customer c
                LEFT JOIN h.currentFp fp
                JOIN c.customerFp cfp
                JOIN cfp.organization org
                """ + buildWhereClause(accessScope);

        TypedQuery<Long> countQuery =
                entityManager.createQuery(countJpql, Long.class);

        bindParams(countQuery, accessScope, status, requestType, customerName);

        return new PageImpl<>(
                contentQuery.getResultList(),
                pageable,
                countQuery.getSingleResult()
        );
    }

    // WHERE 절 동적 생성
    // accessScope에 따라 본인 지점 or 전체 지점 조건 추가
    private String buildWhereClause(AccessScope accessScope) {
        StringBuilder where = new StringBuilder("""
                WHERE h.deletedAt IS NULL
                  AND c.deletedAt IS NULL
                  AND (:customerName IS NULL OR c.customerName LIKE CONCAT('%', :customerName, '%'))
                  AND (:status IS NULL OR h.requestStatus = :status)
                  AND (:requestType IS NULL OR h.requestType = :requestType)
                """);

        if (accessScope.isOwnScope()) {
            where.append("\n  AND cfp.id = :userId");
        } else if (accessScope.isBranchScope()) {
            where.append("\n  AND org.id = :organizationId");
        }

        return where.toString();
    }

    // 쿼리 파라미터 바인딩
    private void bindParams(TypedQuery<?> query, AccessScope accessScope,
                            RequestStatus status, RequestType requestType, String customerName) {
        query.setParameter("customerName", customerName);
        query.setParameter("status", status);
        query.setParameter("requestType", requestType);

        if (accessScope.isOwnScope()) {
            query.setParameter("userId", accessScope.userId());
        } else if (accessScope.isBranchScope()) {
            query.setParameter("organizationId", accessScope.organizationId());
        }
    }

    @Override
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

    @Override
    public String findMainChannel(UUID customerId) {
        List<String> result = entityManager.createQuery("""
        SELECT c.consultationChannel
        FROM Consultation c
        WHERE c.customer.id = :customerId
        AND c.deletedAt IS NULL
        GROUP BY c.consultationChannel
        ORDER BY COUNT(c.id) DESC
        """, String.class)
                .setParameter("customerId", customerId)
                .setMaxResults(1)
                .getResultList();
        return result.isEmpty() ? null : result.get(0);
    }
}