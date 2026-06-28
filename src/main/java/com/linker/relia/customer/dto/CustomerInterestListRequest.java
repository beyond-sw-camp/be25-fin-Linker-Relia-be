package com.linker.relia.customer.dto;

import com.linker.relia.common.dto.request.PageQueryRequest;
import com.linker.relia.customer.domain.InterestReason;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
public class CustomerInterestListRequest extends PageQueryRequest {
    private String customerName;
    private String organizationCode;
    private InterestReason interestReason;
    private String sort;

    @Override
    public Pageable toPageable() {
        return PageRequest.of(getPage() - 1, getSize(), resolveSort());
    }

    private Sort resolveSort() {
        if (sort == null || sort.isBlank()) {
            return Sort.unsorted();
        }

        String[] tokens = sort.split(",");
        if (tokens.length != 2) {
            return Sort.unsorted();
        }

        String property = tokens[0].trim();
        String directionToken = tokens[1].trim();
        if (property.isEmpty() || directionToken.isEmpty()) {
            return Sort.unsorted();
        }

        try {
            return Sort.by(Sort.Direction.fromString(directionToken), property);
        } catch (IllegalArgumentException e) {
            return Sort.unsorted();
        }
    }
}
