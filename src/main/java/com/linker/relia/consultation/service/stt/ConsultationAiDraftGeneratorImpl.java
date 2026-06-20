package com.linker.relia.consultation.service.stt;

import com.linker.relia.consultation.domain.ConsultationType;
import com.linker.relia.consultation.domain.stt.ConsultationSttSession;
import com.linker.relia.consultation.dto.response.ConsultationAiGenerationResult;
import com.linker.relia.consultation.dto.response.ConsultationAiStructuredDraft;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConsultationAiDraftGeneratorImpl implements ConsultationAiDraftGenerator {
    private final ChatClient chatClient;

    public ConsultationAiDraftGeneratorImpl(ChatModel chatModel) {
        this.chatClient = ChatClient.create(chatModel);
    }

    @Override
    public ConsultationAiGenerationResult generate(ConsultationSttSession session, String sttRawText) {
        BeanOutputConverter<ConsultationAiGenerationResult> outputConverter =
                new BeanOutputConverter<>(ConsultationAiGenerationResult.class);

        String response = chatClient.prompt()
                .system(buildSystemPrompt(outputConverter.getFormat(), session.getConsultationType()))
                .user(buildUserPrompt(session, sttRawText))
                .call()
                .content();

        return outputConverter.convert(response);
    }

    private String buildSystemPrompt(String outputFormat, ConsultationType consultationType) {
        return """
                너는 보험 상담 내용을 구조화하는 백엔드 AI 추출기다.
                반환은 반드시 JSON 하나만 해야 한다.
                설명 문장, 마크다운, 코드블록은 절대 포함하지 마라.
                값이 확실하지 않으면 추측하지 말고 null 로 둬라.
                UUID, 상품코드, 질병코드, 계약ID, 고객ID, 일정 시간은 대화에 근거가 없으면 만들지 마라.
                상담 유형은 반드시 %s 로 맞춘다.

                top-level 필드:
                - consultationType
                - consultationChannel: VISIT, PHONE 중 하나 또는 null
                - consultedAt: ISO-8601 datetime 또는 null
                - specialNote
                - nextScheduledAt: ISO-8601 datetime 또는 null
                - customerId
                - contractId
                - customerInfo
                - newDetail
                - claimDetail
                - renewalDetail
                - cancelDetail

                규칙:
                - NEW_CONTRACT 상담에서 기존 고객으로 보이면 customerInfo 는 null 로 둔다.
                - NEW_CONTRACT 상담에서 잠재 고객 정보가 충분하면 customerInfo 를 채운다.
                - CLAIM 는 claimDetail 만 채우고 나머지 상세 객체는 null 로 둔다.
                - RENEWAL 는 renewalDetail 만 채우고 나머지 상세 객체는 null 로 둔다.
                - TERMINATION 는 cancelDetail 만 채우고 나머지 상세 객체는 null 로 둔다.
                - NEW_CONTRACT 는 newDetail 만 채우고 나머지 상세 객체는 null 로 둔다.
                - 배열 값은 근거가 있을 때만 채운다.
                - summaryText 는 한국어 3~5문장으로 상담 핵심, 요청사항, 후속 액션을 요약한다.

                허용 enum 예시:
                - consultationType: NEW_CONTRACT, CLAIM, TERMINATION, RENEWAL
                - consultationChannel: VISIT, PHONE
                - customerMaritalStatus: SINGLE, MARRIED, DIVORCED, WIDOWED
                - retentionPossibility: LOW, MEDIUM, HIGH

                출력 형식:
                %s
                """.formatted(consultationType.name(), outputFormat);
    }

    private String buildUserPrompt(ConsultationSttSession session, String sttRawText) {
        String customerId = session.getCustomer() != null ? session.getCustomer().getId().toString() : "null";

        return """
                아래는 보험 상담 STT 원문이다.

                이미 알고 있는 메타데이터:
                - consultationType: %s
                - knownCustomerId: %s
                - startedAt: %s

                STT transcript:
                %s
                """.formatted(
                session.getConsultationType().name(),
                customerId,
                session.getStartedAt(),
                sttRawText
        );
    }
}
