package com.linker.relia.insurance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "insurance_categories")
public class InsuranceCategory {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @Column(name = "insurance_category_code")
    private String insuranceCategoryCode;

    @Column(name = "insurance_category_name")
    private String insuranceCategoryName;

    @Column(name = "insurance_category_status")
    private String insuranceCategoryStatus;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}

