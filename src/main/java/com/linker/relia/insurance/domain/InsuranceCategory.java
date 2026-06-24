package com.linker.relia.insurance.domain;

import com.linker.relia.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "insurance_categories")
public class InsuranceCategory extends BaseEntity {
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

    public void update(String insuranceCategoryName, String insuranceCategoryStatus) {
        if (insuranceCategoryName != null) {
            this.insuranceCategoryName = insuranceCategoryName;
        }

        if (insuranceCategoryStatus == null) {
            return;
        }

        this.insuranceCategoryStatus = insuranceCategoryStatus;

        if ("INACTIVE".equals(insuranceCategoryStatus)) {
            this.deletedAt = LocalDateTime.now();
            return;
        }

        this.deletedAt = null;
    }
}
