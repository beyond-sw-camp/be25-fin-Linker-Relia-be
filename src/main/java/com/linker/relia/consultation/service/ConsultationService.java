package com.linker.relia.consultation.service;

import com.linker.relia.consultation.dto.request.ConsultationCreateRequest;
import com.linker.relia.consultation.dto.response.ConsultationCreateResponse;
import com.linker.relia.consultation.dto.response.ConsultationListResponse;
import com.linker.relia.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ConsultationService {
    /**
     * Creates a new consultation from the provided request and associated user.
     *
     * @param request the DTO containing consultation creation data
     * @param fp the user performing the operation
     * @return a {@code ConsultationCreateResponse} representing the created consultation
     */
    ConsultationCreateResponse createConsultation(
            ConsultationCreateRequest request,
            User fp
    );

    /**
 * Retrieves a paginated list of consultations according to the provided paging and sorting parameters.
 *
 * @param pageable pagination and sorting information to apply
 * @return a page of ConsultationListResponse objects representing consultations for the requested page
 */
Page<ConsultationListResponse> getConsultations(Pageable pageable);
}