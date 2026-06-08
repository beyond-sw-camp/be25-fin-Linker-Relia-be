package com.linker.relia.consultation.service;

import com.linker.relia.consultation.domain.Consultation;
import com.linker.relia.consultation.dto.request.ConsultationCreateRequest;
import com.linker.relia.consultation.dto.response.ConsultationCreateResponse;
import com.linker.relia.consultation.repository.ConsultationRepository;
import com.linker.relia.customer.domain.Customer;
import com.linker.relia.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final CustomerRepository customerRepository;

    public ConsultationCreateResponse createConsultation(
            ConsultationCreateRequest request,
            UUID fpId
    ) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("고객이 존재하지 않습니다."));

        int nextSequence = consultationRepository
                .findMaxSequenceByCustomerId(request.getCustomerId())
                .orElse(0) + 1;

        LocalDateTime now = LocalDateTime.now();

        Consultation consultation = Consultation.builder()
                .consultationSequence(nextSequence)
                .customer(customer)
                .fpId(fpId)
                .contractId(request.getContractId())
                .consultationType(request.getConsultationType())
                .consultationChannel(request.getConsultationChannel())
                .consultedAt(request.getConsultedAt())
                .specialNote(request.getSpecialNote())
                .nextScheduledAt(request.getNextScheduledAt())
                .createdAt(now)
                .createdBy(fpId)
                .updatedAt(now)
                .updatedBy(fpId)
                .build();

        consultationRepository.save(consultation);

        return new ConsultationCreateResponse(
                consultation.getId()
        );
    }
}
