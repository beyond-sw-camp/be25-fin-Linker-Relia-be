package com.linker.relia.common.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Getter
@Setter
public class PageQueryRequest {
    @Min(value = 1, message = "page는 1 이상이어야 합니다.")
    private Integer page = 1;

    @Min(value = 1, message = "size는 1 이상이어야 합니다.")
    private Integer size = 10;

    public Pageable toPageable() {
        return PageRequest.of(page - 1, size);
    }
}
