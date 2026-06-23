package com.linker.relia.consultation.service.ai;

import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.consultation.domain.ConsultationAiBriefing;
import com.linker.relia.consultation.exception.ConsultationErrorCode;
import com.linker.relia.consultation.repository.ConsultationAiBriefingRepository;
import com.linker.relia.customer.domain.Customer;
import com.linker.relia.customer.dto.CustomerAiBriefingResponse;
import com.linker.relia.customer.exception.CustomerErrorCode;
import com.linker.relia.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultationAiBriefingPersistenceService {

    private final CustomerRepository customerRepository;
    private final ConsultationAiBriefingRepository consultationAiBriefingRepository;

    @Transactional
    public CustomerAiBriefingResponse save(UUID customerId,
                                            String briefingContent,
                                            String sourceFingerprint,
                                            int expectedLatestSequence) {
        Customer customer = customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUSTOMER_NOT_FOUND));

        ConsultationAiBriefing latestBriefing = consultationAiBriefingRepository
                .findFirstByCustomer_IdAndDeletedAtIsNullOrderByUpdateSequenceDescCreatedAtDesc(customerId)
                .orElse(null);
        int currentLatestSequence = latestBriefing == null ? 0 : latestBriefing.getUpdateSequence();

        if (currentLatestSequence != expectedLatestSequence) {
            throw new BusinessException(ConsultationErrorCode.CONSULTATION_AI_BRIEFING_UP_TO_DATE);
        }

        Optional.ofNullable(latestBriefing)
                .filter(briefing -> briefing.getSourceFingerprint().equals(sourceFingerprint))
                .ifPresent(briefing -> {
                    throw new BusinessException(ConsultationErrorCode.CONSULTATION_AI_BRIEFING_UP_TO_DATE);
                });

        int nextSequence = consultationAiBriefingRepository.findMaxUpdateSequenceByCustomerId(customerId) + 1;

        ConsultationAiBriefing briefing = ConsultationAiBriefing.builder()
                .customer(customer)
                .updateSequence(nextSequence)
                .briefingContent(briefingContent)
                .sourceFingerprint(sourceFingerprint)
                .build();

        ConsultationAiBriefing savedBriefing = consultationAiBriefingRepository.save(briefing);

        return new CustomerAiBriefingResponse(
                savedBriefing.getId(),
                savedBriefing.getBriefingContent(),
                savedBriefing.getCreatedAt(),
                false
        );
    }
}
