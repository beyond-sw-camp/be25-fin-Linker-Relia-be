package com.linker.relia.handover.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.handover.exception.HandoverErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 인수인계 결재 화면에 노출되는 "추천 이유 멘트"를 Claude API로 생성한다.
 * 점수 계산(RecommendationService)과 책임을 분리해, 외부 API 의존성을
 * 별도 클래스로 격리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationCommentService {

    private final WebClient openAiWebClient;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.chat.model}")
    private String model;

    private static final String SYSTEM_PROMPT = """
            당신은 보험 GA의 인수인계 결재 화면에서, 추천된 설계사가 왜 적합한지
            한국어로 짧게 설명하는 역할입니다.

            규칙:
            - 1~2문장으로만 작성하세요. 절대 3문장을 넘기지 마세요.
            - 전체 길이는 한국어 기준 80자를 넘기지 마세요.
            - 입력으로 주어진 점수 항목(breakdown) 중, points가 높은 항목 1~2개만
              골라서 근거로 사용하세요. 모든 항목을 나열하지 마세요.
            - "40점", "27점" 같은 점수 숫자는 절대 언급하지 마세요. 대신 그 항목이
              의미하는 내용(예: 유지율 92.5%, 보종 전문성)을 자연스러운 문장으로 녹여서 표현하세요.
            - 'VISIT'은 '방문' 'PHONE'은 '전화' 'MESSAGE'는 '문자'라고 언급하세요
            - 다른 후보자, 탈락 이유, 페널티에 대해서는 절대 언급하지 마세요.
            - 점수가 매칭되지 않은 항목(matched: false, points: 0)은 언급하지 마세요.
            - 결재자가 빠르게 읽고 승인 여부를 판단할 수 있도록, 신뢰감 있고
              간결한 어조로 작성하세요.
            - 반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 포함하지 마세요.

            {"recommendationComment": "string"}
            """;

    /**
     * 1위 추천 후보에 대한 추천 이유 멘트를 생성한다.
     * LLM 호출이 실패하면 템플릿 기반 폴백 문장을 반환한다.
     */
    public String generateComment(CustomerCommentContext customer, TopCandidateContext topCandidate) {
        try {
            String userMessage = objectMapper.writeValueAsString(
                    Map.of("customer", customer, "topCandidate", topCandidate)
            );

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "max_tokens", 200,
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", userMessage)
                    )
            );

            String rawResponse = openAiWebClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseComment(rawResponse);

        } catch (Exception e) {
            log.warn("추천 이유 멘트 생성 실패, 폴백 템플릿으로 대체합니다. fpName={}",
                    topCandidate.fpName(), e);
            return buildFallbackComment(topCandidate);
        }
    }

    private String parseComment(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String contentText = root.path("choices").get(0).path("message").path("content").asText();
            JsonNode parsed = objectMapper.readTree(contentText);
            String comment = parsed.path("recommendationComment").asText();

            if (comment.isBlank()) {
                throw new BusinessException(HandoverErrorCode.COMMENT_GENERATION_FAILED);
            }
            return comment;

        } catch (Exception e) {
            throw new BusinessException(HandoverErrorCode.COMMENT_GENERATION_FAILED);
        }
    }

    private String buildFallbackComment(TopCandidateContext candidate) {
        BigDecimal retentionRate = candidate.breakdown().retentionRate().value();
        return String.format("%s님은 유지율 %s%%로 안정적인 고객 관리 역량을 보유하고 있습니다.",
                candidate.fpName(), retentionRate);
    }

    public record CustomerCommentContext(
            String ageGroup,
            String mainChannel,
            List<String> categoryList
    ) {}

    public record TopCandidateContext(
            String fpName,
            int totalScore,
            ScoreBreakdown breakdown
    ) {}

    public record ScoreBreakdown(
            MatchPoint categoryMatch,
            ValuePoint retentionRate,
            MatchPoint ageGroupMatch,
            MatchPoint channelMatch
    ) {}

    public record MatchPoint(boolean matched, int points) {}

    public record ValuePoint(BigDecimal value, int points) {}
}