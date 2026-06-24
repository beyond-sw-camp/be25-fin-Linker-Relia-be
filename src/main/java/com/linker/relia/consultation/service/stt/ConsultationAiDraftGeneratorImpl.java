package com.linker.relia.consultation.service.stt;

import com.linker.relia.consultation.domain.ConsultationType;
import com.linker.relia.consultation.domain.stt.ConsultationSttSession;
import com.linker.relia.consultation.dto.response.ConsultationAiGenerationResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ConsultationAiDraftGeneratorImpl implements ConsultationAiDraftGenerator {
    private static final String SYSTEM_PROMPT_PATH = "prompts/consultation-ai-system-v2.prompt";
    private static final String USER_PROMPT_PATH = "prompts/consultation-ai-user-v2.prompt";
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*(\\{.*})\\s*```", Pattern.DOTALL);

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

        return convertWithRepair(outputConverter, response);
    }

    private ConsultationAiGenerationResult convertWithRepair(
            BeanOutputConverter<ConsultationAiGenerationResult> outputConverter,
            String response
    ) {
        try {
            return outputConverter.convert(response);
        } catch (Exception ignored) {
        }

        String repaired = repairJson(response);
        return outputConverter.convert(repaired);
    }

    private String repairJson(String response) {
        String normalized = response == null ? "" : response.trim();
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(normalized);
        if (matcher.find()) {
            normalized = matcher.group(1).trim();
        }

        int start = normalized.indexOf('{');
        int end = normalized.lastIndexOf('}');
        if (start >= 0 && end > start) {
            normalized = normalized.substring(start, end + 1);
        }

        return normalized
                .replace('\u201c', '"')
                .replace('\u201d', '"')
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replaceAll(",\\s*([}\\]])", "$1");
    }

    private String buildSystemPrompt(String outputFormat, ConsultationType consultationType) {
        return loadPromptTemplate(SYSTEM_PROMPT_PATH)
                .formatted(consultationType.name(), outputFormat);
    }

    private String buildUserPrompt(ConsultationSttSession session, String sttRawText) {
        String customerId = session.getCustomer() != null ? session.getCustomer().getId().toString() : "null";
        return loadPromptTemplate(USER_PROMPT_PATH)
                .formatted(
                        session.getConsultationType().name(),
                        customerId,
                        session.getStartedAt(),
                        sttRawText
                );
    }

    private String loadPromptTemplate(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load prompt template: " + path, e);
        }
    }
}
