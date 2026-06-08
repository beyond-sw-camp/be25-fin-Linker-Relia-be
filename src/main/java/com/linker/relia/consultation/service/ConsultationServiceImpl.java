package com.linker.relia.consultation.service;

import com.linker.relia.consultation.domain.Consultation;
import com.linker.relia.consultation.dto.request.ConsultationCreateRequest;
import com.linker.relia.consultation.dto.response.ConsultationCreateResponse;
import com.linker.relia.consultation.repository.ConsultationRepository;
import com.linker.relia.customer.domain.Customer;
import com.linker.relia.customer.repository.CustomerRepository;
import com.linker.relia.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationServiceImpl implements ConsultationService {
    private final ConsultationRepository consultationRepository;
    private final CustomerRepository customerRepository;

    @Override
    public ConsultationCreateResponse createConsultation(
            ConsultationCreateRequest request,
            User fp
    ) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("고객이 존재하지 않습니다."));

        int nextSequence = consultationRepository
                .findMaxSequenceByCustomerId(request.getCustomerId())
                .orElse(0) + 1;

        Consultation consultation = Consultation.builder()
                .consultationSequence(nextSequence)
                .customer(customer)
                .fp(fp)
                .contractId(request.getContractId())
                .consultationType(request.getConsultationType())
                .consultationChannel(request.getConsultationChannel())
                .consultedAt(request.getConsultedAt())
                .specialNote(request.getSpecialNote())
                .nextScheduledAt(request.getNextScheduledAt())
                .build();

        consultationRepository.save(consultation);

        return new ConsultationCreateResponse(
                consultation.getId()
        );

    }
}
