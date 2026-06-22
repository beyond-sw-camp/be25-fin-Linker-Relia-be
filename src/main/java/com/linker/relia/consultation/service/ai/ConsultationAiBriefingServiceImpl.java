package com.linker.relia.consultation.service.ai;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.consultation.domain.ConsultationAiBriefing;
import com.linker.relia.consultation.dto.response.ConsultationAiBriefingSourceResponse;
import com.linker.relia.consultation.repository.ConsultationAiBriefingRepository;
import com.linker.relia.consultation.repository.ConsultationRepository;
import com.linker.relia.customer.domain.Customer;
import com.linker.relia.customer.dto.CustomerAiBriefingResponse;
import com.linker.relia.customer.exception.CustomerErrorCode;
import com.linker.relia.customer.repository.CustomerRepository;
import com.linker.relia.customer.service.CustomerAccessService;
import com.linker.relia.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultationAiBriefingServiceImpl implements ConsultationAiBriefingService {

    private static final int AI_BRIEFING_CONSULTATION_LIMIT = 10;

    private final CustomerRepository customerRepository;
    private final CustomerAccessService customerAccessService;
    private final ConsultationRepository consultationRepository;
    private final ConsultationAiBriefingRepository consultationAiBriefingRepository;
    private final ConsultationAiBriefingGenerator consultationAiBriefingGenerator;

    @Override
    @Transactional
    public CustomerAiBriefingResponse generateAiBriefing(PrincipalDetails principalDetails, UUID customerId) {
        AccessScope accessScope = customerAccessService.resolveAccessScope(principalDetails);
        customerAccessService.validateCustomerAccess(accessScope, customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUSTOMER_NOT_FOUND));

        List<ConsultationAiBriefingSourceResponse> consultations =
                consultationRepository.findConsultationsForAiBriefing(
                        accessScope,
                        customerId,
                        AI_BRIEFING_CONSULTATION_LIMIT
                );

        String prompt = buildUserPrompt(customer, consultations);
        String briefingContent = consultationAiBriefingGenerator.generate(prompt);

        Integer maxSequence = consultationAiBriefingRepository.findMaxUpdateSequenceByCustomerId(customerId);
        int nextSequence = maxSequence + 1;

        ConsultationAiBriefing briefing = ConsultationAiBriefing.builder()
                .customer(customer)
                .updateSequence(nextSequence)
                .briefingContent(briefingContent)
                .build();

        ConsultationAiBriefing savedBriefing = consultationAiBriefingRepository.save(briefing);

        return new CustomerAiBriefingResponse(
                savedBriefing.getId(),
                savedBriefing.getBriefingContent(),
                savedBriefing.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerAiBriefingResponse getLatestAiBriefing(PrincipalDetails principalDetails, UUID customerId) {
        AccessScope accessScope = customerAccessService.resolveAccessScope(principalDetails);
        customerAccessService.validateCustomerAccess(accessScope, customerId);

        return consultationAiBriefingRepository.findOwnCustomerLatestAiBriefing(customerId)
                .orElseGet(CustomerAiBriefingResponse::empty);
    }

    private String buildUserPrompt(Customer customer, List<ConsultationAiBriefingSourceResponse> consultations) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("고객명: ")
                .append(customer.getCustomerName())
                .append("\n\n");

        prompt.append("아래는 고객의 최근 상담 이력이다.\n");
        prompt.append("상담 이력에 근거해서만 AI 상담 히스토리 브리핑을 작성하라.\n\n");

        if (consultations.isEmpty()) {
            prompt.append("상담 이력 없음\n");
            return prompt.toString();
        }

        for (ConsultationAiBriefingSourceResponse consultation : consultations) {
            prompt.append("상담일: ")
                    .append(consultation.getConsultedAt())
                    .append("\n");

            prompt.append("상담유형: ")
                    .append(consultation.getConsultationType())
                    .append("\n");

            prompt.append("상담채널: ")
                    .append(consultation.getConsultationChannel())
                    .append("\n");

            prompt.append("담당FP: ")
                    .append(consultation.getFpName())
                    .append("\n");

            prompt.append("특이사항: ")
                    .append(consultation.getSpecialNote() == null ? "없음" : consultation.getSpecialNote())
                    .append("\n");

            prompt.append("다음 상담 예정일: ")
                    .append(consultation.getNextScheduledAt() == null ? "없음" : consultation.getNextScheduledAt())
                    .append("\n\n");
        }

        return prompt.toString();
    }
}