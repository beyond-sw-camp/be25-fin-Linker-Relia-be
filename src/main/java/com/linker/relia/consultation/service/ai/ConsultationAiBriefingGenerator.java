package com.linker.relia.consultation.service.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class ConsultationAiBriefingGenerator {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(25);

    private final ChatClient chatClient;

    public ConsultationAiBriefingGenerator(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.chat.model}") String model,
            @Value("${spring.ai.openai.base-url:https://api.openai.com}") String baseUrl,
            ToolCallingManager toolCallingManager,
            RetryTemplate retryTemplate,
            ResponseErrorHandler responseErrorHandler
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                .webClientBuilder(WebClient.builder())
                .responseErrorHandler(responseErrorHandler)
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                .toolCallingManager(toolCallingManager)
                .retryTemplate(retryTemplate)
                .build();

        this.chatClient = ChatClient.create(chatModel);
    }

    public String generate(String prompt, UUID customerId, int consultationCount) {
        Instant startedAt = Instant.now();
        long startedNanos = System.nanoTime();

        log.info(
                "AI briefing OpenAI API call started customerId={} consultationCount={} promptLength={} startedAt={} readTimeoutSeconds={} connectTimeoutSeconds={}",
                customerId,
                consultationCount,
                prompt.length(),
                startedAt,
                READ_TIMEOUT.toSeconds(),
                CONNECT_TIMEOUT.toSeconds()
        );

        String content;
        try {
            content = chatClient.prompt()
                    .system(buildSystemPrompt())
                    .user(prompt)
                    .call()
                    .content();

            if (!StringUtils.hasText(content)) {
                throw new IllegalStateException("AI briefing response must not be blank");
            }
        } catch (RuntimeException exception) {
            Instant endedAt = Instant.now();
            long durationMillis = Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
            log.warn(
                    "AI briefing OpenAI API call failed customerId={} consultationCount={} promptLength={} startedAt={} endedAt={} durationMs={} exceptionType={} exceptionMessage={}",
                    customerId,
                    consultationCount,
                    prompt.length(),
                    startedAt,
                    endedAt,
                    durationMillis,
                    exception.getClass().getName(),
                    exception.getMessage(),
                    exception
            );
            throw exception;
        }

        Instant endedAt = Instant.now();
        long durationMillis = Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
        log.info(
                "AI briefing OpenAI API call completed customerId={} consultationCount={} promptLength={} startedAt={} endedAt={} durationMs={}",
                customerId,
                consultationCount,
                prompt.length(),
                startedAt,
                endedAt,
                durationMillis
        );

        return content;
    }

    private String buildSystemPrompt() {
        return """
                너는 보험 설계사 인수인계를 돕고 고객 상담의 히스토리를 분석하여 상담의 각 상담 회차 사이의 맥락을 파악하도록 돕는  AI 브리핑 생성기다.

                제공된 고객의 상담 이력을 분석하여 아래 형식으로 작성하라.

                [고객 상담 핵심 요약]
                
                현재까지의 상담 내용을 2~3문장으로 요약
                
                [주요 상담 이력 분석]
                
                고객이 반복적으로 언급한 관심사
                상담 과정에서 나타난 성향
                보험 유지/변경/청구 관련 주요 이슈
                
                [반복 언급 키워드]
                
                3~5개의 핵심 키워드를 쉼표로 구분하여 작성
                
                [브리핑 요약]
                
                새로운 담당 설계사가 다음 상담 시 참고해야 할 사항
                추천 후속 상담 방향
                주의해야 할 고객 성향
                
                규칙:
                
                반드시 한국어로 작성한다.
                상담 이력에 없는 사실은 추측하지 않는다.
                보험 설계사가 바로 참고할 수 있도록 작성한다.
                마크다운 코드블록은 사용하지 않는다.
                각 항목 제목은 반드시 유지한다.
                """;
    }
}
