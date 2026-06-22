package com.linker.relia.consultation.service.stt;

import com.linker.relia.consultation.domain.ConsultationType;
import com.linker.relia.consultation.domain.stt.ConsultationSttSession;
import com.linker.relia.consultation.dto.response.ConsultationAiGenerationResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

@Service
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
                너는 보험 상담 STT 전사본을 최종 저장 직전 단계의 구조화 초안으로 변환하는 추출기다.
                반드시 JSON 하나만 반환한다.
                설명 문장, 마크다운, 코드블록은 절대 포함하지 않는다.

                가장 중요한 원칙:
                - 화면 표시값이 아니라 저장값 기준으로 채운다.
                - 모르면 추측하지 말고 null 로 둔다.
                - STT 또는 메타데이터에 근거가 없는 UUID, 계약 ID, 고객 ID, 상품 코드, 질병 코드는 만들지 않는다.
                - 상담 유형은 반드시 %s 로 맞춘다.

                top-level 필드:
                - consultationType
                - consultationChannel
                - consultedAt
                - specialNote
                - nextScheduledAt
                - customerId
                - contractId
                - customerInfo
                - newDetail
                - claimDetail
                - renewalDetail
                - cancelDetail

                저장 형식 기준:
                - customerId: UUID
                - contractId: UUID
                - consultationType: NEW_CONTRACT, CLAIM, TERMINATION, RENEWAL
                - consultationChannel: VISIT, PHONE, MESSAGE 중 하나 또는 null
                - customerInfo.customerMaritalStatus: SINGLE, MARRIED
                - customerInfo.underlyingDiseaseCodes: 질병명이 아니라 diseaseCode 배열
                - newDetail.coverageTypes: 보장 니즈 enum 배열
                - newDetail.proposedProductCodes: 상품명이 아니라 insuranceProductCode 배열
                - cancelDetail.retentionPossibility: HIGH, MEDIUM, LOW 중 하나

                공통 규칙:
                - summaryText 는 한국어 3~5문장으로 상담 핵심, 고객 요청, 후속 액션을 요약한다.
                - 구조화 데이터는 실제 저장 요청에 최대한 가깝게 채운다.
                - 배열은 근거가 있는 값만 넣고, 없으면 null 로 둔다.
                - 해당 상담 유형과 무관한 detail 객체는 반드시 null 로 둔다.
                - NEW_CONTRACT 가 아닌데 customerInfo 를 만들지 않는다.
                - knownCustomerId 가 있으면 customerId 에 그대로 사용하고 customerInfo 는 가능한 한 비운다.
                - 대상 계약이 특정되지 않으면 contractId 는 null 로 둔다.
                - 상품명, 보험사명, 질병명 같은 표시용 텍스트를 id/code 필드에 넣지 않는다.

                상담 유형별 규칙:
                - NEW_CONTRACT: newDetail 만 채우고 claimDetail, renewalDetail, cancelDetail 은 null
                - CLAIM: claimDetail 만 채우고 나머지 detail 은 null
                - RENEWAL: renewalDetail 만 채우고 나머지 detail 은 null
                - TERMINATION: cancelDetail 만 채우고 나머지 detail 은 null

                NEW_CONTRACT 규칙:
                - coverageTypes 는 아래 enum 코드만 허용한다:
                  CANCER, HEART, LIFE, DEATH, LONG_TERM_CARE
                - "암", "암 보장", "암 진단비" 계열은 CANCER
                - "심장", "심혈관" 계열은 HEART
                - "생명", "종신" 계열은 LIFE
                - "사망 보장" 계열은 DEATH
                - "장기요양", "간병" 계열은 LONG_TERM_CARE
                - coverageTypes 에 상품명, 상품코드, 자유서술을 넣지 않는다.
                - proposedProductCodes 는 LP003 같은 상품 코드만 허용한다.
                - 상품명만 언급되고 코드 근거가 없으면 proposedProductCodes 는 null 로 둔다.
                - coverageTypes 와 proposedProductCodes 를 섞지 않는다.

                CLAIM 규칙:
                - reviewItems 는 가능하면 아래 코드만 사용한다:
                  COVERAGE_ELIGIBLE, EXEMPTION_PERIOD, EXCLUSION_POSSIBILITY, PREVIOUS_CLAIM_HISTORY
                - claimType, result, hospitalizationStatus, surgeryStatus 는 근거가 있을 때만 채운다.

                RENEWAL 규칙:
                - premiumChangeReasonTypes 는 아래 코드만 사용한다:
                  AGE_INCREASE, RISK_CHANGE, LOSS_RATIO_CHANGE, COVERAGE_CHANGE, OTHER
                - interestTypes 는 아래 코드만 사용한다:
                  PREMIUM, COVERAGE, MATURITY, REFUND, ALTERNATIVE_PRODUCT
                - otherReason 은 premiumChangeReasonTypes 에 OTHER 가 있을 때만 채운다.

                TERMINATION 규칙:
                - retentionPossibility 는 HIGH, MEDIUM, LOW 중 하나만 사용한다.

                customerInfo 규칙:
                - underlyingDiseaseCodes 는 질병명 배열이 아니라 diseaseCode 배열이어야 한다.
                - 질병명만 있고 코드 근거가 없으면 underlyingDiseaseCodes 는 null 로 둔다.

                출력 형식:
                %s
                """.formatted(consultationType.name(), outputFormat);
    }

    private String buildUserPrompt(ConsultationSttSession session, String sttRawText) {
        String customerId = session.getCustomer() != null ? session.getCustomer().getId().toString() : "null";

        return """
                아래는 보험 상담 STT 전사본이다.

                현재 알고 있는 메타데이터:
                - consultationType: %s
                - knownCustomerId: %s
                - startedAt: %s

                작업 목표:
                - 전사본을 상담일지 저장용 structured draft 로 변환한다.
                - 저장값이 아닌 표시값만 알 수 있는 경우 해당 id/code 필드는 null 로 둔다.
                - customerId, contractId, proposedProductCodes, underlyingDiseaseCodes 는 추측 금지다.
                - 최종 출력은 반드시 지정된 JSON 형식만 반환한다.

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
