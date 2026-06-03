package com.linker.relia.consultation.service;

import com.linker.relia.consultation.domain.Consultation;
import com.linker.relia.consultation.dto.request.ConsultationCreateRequest;
import com.linker.relia.consultation.dto.response.ConsultationCreateResponse;
import com.linker.relia.consultation.repository.ConsultationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationService {

    private final ConsultationRepository consultationRepository;

    public ConsultationCreateResponse createConsultation(
            ConsultationCreateRequest request,
            UUID fpId
    ) {
        int nextSequence = consultationRepository
                .findMaxSequenceByCustomerId(request.getCustomerId())
                .orElse(0) + 1;

        Consultation consultation = Consultation.builder()
                .consultationSequence(nextSequence)
                .customerId(request.getCustomerId())
                .fpId(fpId)
                .contractId(request.getContractId())
                .consultationType(request.getConsultationType())
                .consultationChannel(request.getConsultationChannel())
                .consultedAt(request.getConsultedAt())
                .specialNote(request.getSpecialNote())
                .nextScheduledAt(request.getNextScheduledAt())
                .createdAt(java.time.LocalDateTime.now())
                .createdBy(fpId)
                .updatedAt(java.time.LocalDateTime.now())
                .updatedBy(fpId)
                .build();

        consultationRepository.save(consultation);

        return new ConsultationCreateResponse(
                consultation.getId()
        );
    }
}
