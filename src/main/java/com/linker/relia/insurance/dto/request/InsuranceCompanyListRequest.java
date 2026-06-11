package com.linker.relia.insurance.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Getter
@Setter
public class InsuranceCompanyListRequest {
    @NotNull(message = "page는 필수입니다.")
    @Min(value = 1, message = "page는 1 이상이어야 합니다.")
    private Integer page;

    @NotNull(message = "size는 필수입니다.")
    @Min(value = 1, message = "size는 1 이상이어야 합니다.")
    private Integer size;

    private String insuranceCompanyName;

    public Pageable toPageable() {
        return PageRequest.of(page - 1, size);
    }

    public String normalizedInsuranceCompanyName() {
        if (insuranceCompanyName == null) {
            return null;
        }

        String trimmed = insuranceCompanyName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
