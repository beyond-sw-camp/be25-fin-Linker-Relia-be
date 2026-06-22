package com.linker.relia.consultation.repository;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.consultation.dto.response.ConsultationAiBriefingSourceResponse;
import com.linker.relia.consultation.dto.response.ConsultationHistoryItemResponse;
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
public class ConsultationRepositoryImpl implements ConsultationRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<ConsultationHistoryItemResponse> findOwnCustomerConsultations(AccessScope accessScope,
                                                                              UUID customerId,
                                                                              Pageable pageable) {
        String contentJpql = """
                select new com.linker.relia.consultation.dto.response.ConsultationHistoryItemResponse(
                    cs.id,
                    cs.consultationSequence,
                    cs.consultedAt,
                    cs.consultationType,
                    cs.consultationChannel,
                    fp.id,
                    fp.userName,
                    cs.nextScheduledAt
                )
                from Consultation cs
                join cs.customer c
                join c.customerFp customerFp
                join customerFp.organization org
                join cs.fp fp
                """ + buildWhereClause(accessScope) + """
                order by cs.consultedAt desc, cs.createdAt desc
                """;

        TypedQuery<ConsultationHistoryItemResponse> contentQuery =
                entityManager.createQuery(contentJpql, ConsultationHistoryItemResponse.class);
        bindAccessScope(contentQuery, accessScope);
        contentQuery.setParameter("customerId", customerId);
        contentQuery.setFirstResult((int) pageable.getOffset());
        contentQuery.setMaxResults(pageable.getPageSize());

        String countJpql = """
                select count(cs)
                from Consultation cs
                join cs.customer c
                join c.customerFp customerFp
                join customerFp.organization org
                """ + buildWhereClause(accessScope);

        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);
        bindAccessScope(countQuery, accessScope);
        countQuery.setParameter("customerId", customerId);

        List<ConsultationHistoryItemResponse> content = contentQuery.getResultList();
        return new PageImpl<>(content, pageable, countQuery.getSingleResult());
    }

    private void bindAccessScope(TypedQuery<?> query, AccessScope accessScope) {
        if (accessScope.isOwnScope()) {
            query.setParameter("userId", accessScope.userId());
        } else if (accessScope.isBranchScope()) {
            query.setParameter("organizationId", accessScope.organizationId());
        }
    }

    @Override
    public List<ConsultationAiBriefingSourceResponse> findConsultationsForAiBriefing(
            AccessScope accessScope,
            UUID customerId,
            int limit
    ) {
        String jpql = """
            select new com.linker.relia.consultation.dto.response.ConsultationAiBriefingSourceResponse(
                cs.id,
                cs.consultationSequence,
                cs.consultedAt,
                cs.consultationType,
                cs.consultationChannel,
                fp.userName,
                cs.specialNote,
                cs.nextScheduledAt
            )
            from Consultation cs
            join cs.customer c
            join c.customerFp customerFp
            join customerFp.organization org
            join cs.fp fp
            """ + buildWhereClause(accessScope) + """
            order by cs.consultedAt desc, cs.createdAt desc
            """;

        TypedQuery<ConsultationAiBriefingSourceResponse> query =
                entityManager.createQuery(
                        jpql,
                        ConsultationAiBriefingSourceResponse.class
                );

        bindAccessScope(query, accessScope);
        query.setParameter("customerId", customerId);
        query.setMaxResults(limit);

        return query.getResultList();
    }

    private String buildWhereClause(AccessScope accessScope) {
        StringBuilder whereClause = new StringBuilder("""
                where cs.deletedAt is null
                  and c.deletedAt is null
                  and customerFp.deletedAt is null
                  and org.deletedAt is null
                  and cs.customer.id = :customerId
                """);

        if (accessScope.isOwnScope()) {
            whereClause.append("\n  and customerFp.id = :userId");
        } else if (accessScope.isBranchScope()) {
            whereClause.append("\n  and org.id = :organizationId");
        }

        whereClause.append("\n");
        return whereClause.toString();
    }
}
