package com.linker.relia.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_emp_code_sequences")
public class UserEmpCodeSequence {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "user_role")
    private UserRole userRole;

    @Column(name = "next_value")
    private Long nextValue;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public long issueNextValue(LocalDateTime now) {
        long issuedValue = nextValue;
        nextValue = issuedValue + 1;
        updatedAt = now;
        return issuedValue;
    }
}
