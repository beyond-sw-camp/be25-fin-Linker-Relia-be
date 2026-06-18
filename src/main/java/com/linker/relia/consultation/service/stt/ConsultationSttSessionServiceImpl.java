package com.linker.relia.consultation.service.stt;

import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.consultation.domain.stt.ConsultationSttSession;
import com.linker.relia.consultation.domain.stt.ConsultationSttSessionStatus;
import com.linker.relia.consultation.dto.request.ConsultationSttSessionCompleteRequest;
import com.linker.relia.consultation.dto.request.ConsultationSttSessionStartRequest;
import com.linker.relia.consultation.dto.response.ConsultationSttSessionResponse;
import com.linker.relia.consultation.exception.ConsultationErrorCode;
import com.linker.relia.consultation.repository.stt.ConsultationSttSessionRepository;
import com.linker.relia.customer.domain.Customer;
import com.linker.relia.customer.exception.CustomerErrorCode;
import com.linker.relia.customer.repository.CustomerRepository;
import com.linker.relia.user.domain.User;
import com.linker.relia.user.exception.UserErrorCode;
import com.linker.relia.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationSttSessionServiceImpl implements ConsultationSttSessionService {
    private final ConsultationSttSessionRepository consultationSttSessionRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    @Override
    public ConsultationSttSessionResponse startSession(UUID fpId, ConsultationSttSessionStartRequest request) {
        User fp = userRepository.findById(fpId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findByIdAndDeletedAtIsNull(request.getCustomerId())
                    .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUSTOMER_NOT_FOUND));
        }

        ConsultationSttSession session = ConsultationSttSession.builder()
                .customer(customer)
                .fp(fp)
                .consultationType(request.getConsultationType())
                .sessionStatus(ConsultationSttSessionStatus.RECORDING)
                .startedAt(LocalDateTime.now())
                .build();

        return toResponse(consultationSttSessionRepository.save(session));
    }

    @Override
    public ConsultationSttSessionResponse completeSession(UUID sessionId, UUID fpId, ConsultationSttSessionCompleteRequest request) {
        ConsultationSttSession session = getOwnedSession(sessionId, fpId);
        session.complete(request.getFinalText(), LocalDateTime.now());
        return toResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationSttSessionResponse getSession(UUID sessionId, UUID fpId) {
        return toResponse(getOwnedSession(sessionId, fpId));
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationSttSession getOwnedSession(UUID sessionId, UUID fpId) {
        ConsultationSttSession session = consultationSttSessionRepository.findByIdAndDeletedAtIsNull(sessionId)
                .orElseThrow(() -> new BusinessException(ConsultationErrorCode.CONSULTATION_STT_SESSION_NOT_FOUND));

        if (!session.getFp().getId().equals(fpId)) {
            throw new BusinessException(ConsultationErrorCode.CONSULTATION_STT_SESSION_ACCESS_DENIED);
        }

        return session;
    }

    private ConsultationSttSessionResponse toResponse(ConsultationSttSession session) {
        return ConsultationSttSessionResponse.builder()
                .sessionId(session.getId())
                .customerId(session.getCustomer() != null ? session.getCustomer().getId() : null)
                .consultationType(session.getConsultationType())
                .sessionStatus(session.getSessionStatus())
                .partialText(session.getPartialText())
                .finalText(session.getFinalText())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .build();
    }
}
