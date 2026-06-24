package com.linker.relia.insurance.repository;

import com.linker.relia.insurance.dto.response.InsuranceCompanyListItemResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class InsuranceCompanyRepositoryImpl implements InsuranceCompanyRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<InsuranceCompanyListItemResponse> searchPartnerInsuranceCompanies(String insuranceCompanyName,
                                                                                  Pageable pageable) {
        String baseWhereSql = """
                from insurance_companies ic
                left join (
                    select
                        insurance_company_id,
                        coalesce(sum(gross_commission_amount), 0) as income_commission
                    from gross_commission_records
                    group by insurance_company_id
                ) gcr on gcr.insurance_company_id = ic.id
                left join (
                    select
                        ip.insurance_company_id,
                        count(distinct ct.id) as contract_count
                    from contracts ct
                    join insurance_products ip on ip.id = ct.insurance_product_id
                    where ct.deleted_at is null
                      and ip.deleted_at is null
                    group by ip.insurance_company_id
                ) contract_summary on contract_summary.insurance_company_id = ic.id
                where ic.deleted_at is null
                  and (:insuranceCompanyName is null or ic.insurance_company_name like :insuranceCompanyName)
                """;

        String insuranceCompanyNamePattern = toLikePattern(insuranceCompanyName);
        String contentSql = """
                select
                    ic.id,
                    ic.insurance_company_name,
                    date(ic.created_at) as partner_start_date,
                    coalesce(gcr.income_commission, 0) as income_commission,
                    coalesce(contract_summary.contract_count, 0) as contract_count
                """ + baseWhereSql + """
                order by ic.insurance_company_name asc, ic.id asc
                """;

        Query contentQuery = entityManager.createNativeQuery(contentSql);
        contentQuery.setParameter("insuranceCompanyName", insuranceCompanyNamePattern);
        contentQuery.setFirstResult((int) pageable.getOffset());
        contentQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = contentQuery.getResultList();
        List<InsuranceCompanyListItemResponse> content = rows.stream()
                .map(this::toInsuranceCompanyListItemResponse)
                .toList();

        Query countQuery = entityManager.createNativeQuery("""
                select count(*)
                from insurance_companies ic
                where ic.deleted_at is null
                  and (:insuranceCompanyName is null or ic.insurance_company_name like :insuranceCompanyName)
                """);
        countQuery.setParameter("insuranceCompanyName", insuranceCompanyNamePattern);

        return new PageImpl<>(content, pageable, toLong(countQuery.getSingleResult()));
    }

    private String toLikePattern(String insuranceCompanyName) {
        return insuranceCompanyName == null ? null : "%" + insuranceCompanyName + "%";
    }

    private InsuranceCompanyListItemResponse toInsuranceCompanyListItemResponse(Object[] row) {
        return InsuranceCompanyListItemResponse.builder()
                .insuranceCompanyId(toUuid(row[0]))
                .insuranceCompanyName((String) row[1])
                .partnerStartDate(toLocalDate(row[2]))
                .incomeCommission(toLong(row[3]))
                .contractCount(toLong(row[4]))
                .build();
    }

    private UUID toUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }

        return UUID.fromString(Objects.requireNonNull(value).toString());
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof Date date) {
            return date.toLocalDate();
        }

        return (LocalDate) value;
    }

    private long toLong(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.longValue();
        }

        return ((Number) Objects.requireNonNull(value)).longValue();
    }
}
