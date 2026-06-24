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

    // FP와 고객 정보를 검증한 뒤 STT 세션을 RECORDING 상태로 생성한다.
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

    // 수동 완료 API는 프론트가 최종 텍스트를 확정했을 때 세션을 종료하는 용도다.
    @Override
    public ConsultationSttSessionResponse completeSession(UUID sessionId, UUID fpId, ConsultationSttSessionCompleteRequest request) {
        ConsultationSttSession session = getOwnedSession(sessionId, fpId);
        session.complete(request.getFinalText(), LocalDateTime.now());
        return toResponse(session);
    }

    // 조회 시에는 소유권까지 함께 확인한 뒤 응답 DTO로 변환한다.
    @Override
    @Transactional(readOnly = true)
    public ConsultationSttSessionResponse getSession(UUID sessionId, UUID fpId) {
        return toResponse(getOwnedSession(sessionId, fpId));
    }

    // gRPC 콜백 스레드에서도 중간 전사 결과가 저장되도록 별도 서비스 메서드로 분리했다.
    @Override
    public void updatePartialText(UUID sessionId, UUID fpId, String partialText) {
        ConsultationSttSession session = getOwnedSession(sessionId, fpId);
        session.updatePartialText(partialText);
    }

    // 최종 전사 결과를 저장하고 세션을 COMPLETED로 전환한다.
    @Override
    public void completeSession(UUID sessionId, UUID fpId, String finalText, LocalDateTime endedAt) {
        ConsultationSttSession session = getOwnedSession(sessionId, fpId);
        session.complete(finalText, endedAt);
    }

    // STT 처리 실패 시 에러 메시지와 종료 시각을 함께 저장한다.
    @Override
    public void failSession(UUID sessionId, UUID fpId, String errorMessage, LocalDateTime endedAt) {
        ConsultationSttSession session = getOwnedSession(sessionId, fpId);
        session.fail(errorMessage, endedAt);
    }

    // 세션 존재 여부와 소유권을 함께 검증하는 공통 조회 메서드다.
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

    // 엔티티를 프론트 응답용 DTO로 평탄화한다.
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
