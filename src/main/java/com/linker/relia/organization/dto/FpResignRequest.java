package com.linker.relia.organization.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class FpResignRequest {
    @NotNull(message = "resignedAt은 필수입니다.")
    private LocalDate resignedAt;
}
