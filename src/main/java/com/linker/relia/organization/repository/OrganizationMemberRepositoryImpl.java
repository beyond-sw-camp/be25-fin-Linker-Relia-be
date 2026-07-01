package com.linker.relia.organization.repository;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.organization.dto.OrganizationMemberItemResponse;
import com.linker.relia.organization.dto.OrganizationMemberSort;
import com.linker.relia.user.domain.UserRole;
import com.linker.relia.user.domain.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class OrganizationMemberRepositoryImpl implements OrganizationMemberRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<OrganizationMemberItemResponse> searchMembers(AccessScope accessScope,
                                                              String keyword,
                                                              String branchKeyword,
                                                              UUID organizationId,
                                                              UserRole role,
                                                              UserStatus status,
                                                              OrganizationMemberSort sort,
                                                              boolean includeResigned,
                                                              Pageable pageable) {
        String fromWhereSql = buildFromWhereSql(accessScope);
        String orderBySql = buildOrderBySql(sort);

        String contentSql = """
                select
                    u.id,
                    u.emp_code,
                    u.user_name,
                    org.id as organization_id,
                    org.organization_name,
                    u.user_role,
                    u.email,
                    u.phone,
                    u.user_status
                """ + fromWhereSql + orderBySql;

        Query contentQuery = entityManager.createNativeQuery(contentSql);
        bindParameters(
                contentQuery,
                accessScope,
                keyword,
                branchKeyword,
                organizationId,
                role,
                status,
                includeResigned
        );
        contentQuery.setFirstResult((int) pageable.getOffset());
        contentQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = contentQuery.getResultList();
        List<OrganizationMemberItemResponse> content = rows.stream()
                .map(this::toOrganizationMemberItemResponse)
                .toList();

        Query countQuery = entityManager.createNativeQuery("select count(*) " + fromWhereSql);
        bindParameters(
                countQuery,
                accessScope,
                keyword,
                branchKeyword,
                organizationId,
                role,
                status,
                includeResigned
        );

        return new PageImpl<>(content, pageable, toLong(countQuery.getSingleResult()));
    }

    private String buildFromWhereSql(AccessScope accessScope) {
        return """
                from users u
                join organizations org on org.id = u.organization_id
                where u.deleted_at is null
                  and org.deleted_at is null
                  and u.user_role in ('HQ_MANAGER', 'BRANCH_MANAGER', 'FP')
                  and (:includeResigned = true or u.user_status = 'ACTIVE')
                  and (:keyword is null or u.user_name like concat('%', :keyword, '%'))
                  and (:branchKeyword is null or org.organization_name like concat('%', :branchKeyword, '%'))
                  and (:organizationId is null or org.id = :organizationId)
                  and (:role is null or u.user_role = :role)
                  and (:status is null or u.user_status = :status)
                """ + buildAccessScopeWhereClause(accessScope);
    }

    private String buildAccessScopeWhereClause(AccessScope accessScope) {
        if (accessScope.isOwnScope() || accessScope.isBranchScope()) {
            return "  and org.id = :accessOrganizationId\n";
        }

        return "";
    }

    private String buildOrderBySql(OrganizationMemberSort sort) {
        OrganizationMemberSort resolvedSort = sort == null ? OrganizationMemberSort.NAME_ASC : sort;

        return switch (resolvedSort) {
            case NAME_DESC -> "order by u.user_name desc, u.emp_code asc, u.id asc\n";
            case BRANCH_ASC -> "order by org.organization_name asc, u.user_role asc, u.user_name asc, u.id asc\n";
            case ROLE_ASC -> """
                    order by
                        case u.user_role
                            when 'HQ_MANAGER' then 1
                            when 'BRANCH_MANAGER' then 2
                            when 'FP' then 3
                            else 4
                        end asc,
                        org.organization_name asc,
                        u.user_name asc,
                        u.id asc
                    """;
            case EMP_CODE_ASC -> "order by u.emp_code asc, u.user_name asc, u.id asc\n";
            case NAME_ASC -> "order by u.user_name asc, u.emp_code asc, u.id asc\n";
        };
    }

    private void bindParameters(Query query,
                                AccessScope accessScope,
                                String keyword,
                                String branchKeyword,
                                UUID organizationId,
                                UserRole role,
                                UserStatus status,
                                boolean includeResigned) {
        query.setParameter("keyword", keyword);
        query.setParameter("branchKeyword", branchKeyword);
        query.setParameter("organizationId", organizationId == null ? null : organizationId.toString());
        query.setParameter("role", role == null ? null : role.name());
        query.setParameter("status", status == null ? null : status.name());
        query.setParameter("includeResigned", includeResigned);

        if (accessScope.isOwnScope() || accessScope.isBranchScope()) {
            query.setParameter("accessOrganizationId", accessScope.organizationId().toString());
        }
    }

    private OrganizationMemberItemResponse toOrganizationMemberItemResponse(Object[] row) {
        return OrganizationMemberItemResponse.builder()
                .id(toUuid(row[0]))
                .fpId(toFpId(row))
                .empCode((String) row[1])
                .userName((String) row[2])
                .organizationId(toUuid(row[3]))
                .organizationName((String) row[4])
                .userRole(toUserRole(row[5]))
                .email((String) row[6])
                .phone((String) row[7])
                .userStatus(toUserStatus(row[8]))
                .build();
    }

    private UUID toFpId(Object[] row) {
        UserRole userRole = toUserRole(row[5]);
        if (userRole != UserRole.FP) {
            return null;
        }

        return toUuid(row[0]);
    }

    private UUID toUuid(Object value) {
        return value == null ? null : UUID.fromString(value.toString());
    }

    private UserRole toUserRole(Object value) {
        return value == null ? null : UserRole.valueOf(value.toString());
    }

    private UserStatus toUserStatus(Object value) {
        return value == null ? null : UserStatus.valueOf(value.toString());
    }

    private long toLong(Object value) {
        return ((Number) Objects.requireNonNull(value)).longValue();
    }
}
