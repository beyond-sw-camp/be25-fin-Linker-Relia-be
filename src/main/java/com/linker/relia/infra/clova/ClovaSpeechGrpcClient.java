package com.linker.relia.infra.clova;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.linker.relia.infra.config.ClovaSttProperties;
import com.nbp.cdncp.nest.grpc.proto.v1.NestConfig;
import com.nbp.cdncp.nest.grpc.proto.v1.NestData;
import com.nbp.cdncp.nest.grpc.proto.v1.NestRequest;
import com.nbp.cdncp.nest.grpc.proto.v1.NestResponse;
import com.nbp.cdncp.nest.grpc.proto.v1.NestServiceGrpc;
import com.nbp.cdncp.nest.grpc.proto.v1.RequestType;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

// CLOVA Speech gRPC 스트림 생성과 응답 파싱을 담당한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class ClovaSpeechGrpcClient {
    private static final String DEFAULT_LANGUAGE = "ko";
    private static final long FINAL_RESPONSE_WAIT_MS = 1500L;

    private final ClovaSttProperties clovaSttProperties;
    private final ObjectMapper objectMapper;

    public ClovaSpeechStream openStream(
            UUID sessionId,
            Consumer<String> partialTextConsumer,
            Consumer<String> finalTextConsumer,
            Consumer<Throwable> errorConsumer
    ) {
        ManagedChannel channel = buildChannel();
        NestServiceGrpc.NestServiceStub stub = NestServiceGrpc.newStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(buildMetadata()));

        ResponseAccumulator accumulator = new ResponseAccumulator(partialTextConsumer);
        AtomicBoolean terminated = new AtomicBoolean(false);

        StreamObserver<NestRequest> requestObserver = stub.recognize(new StreamObserver<>() {
            @Override
            public void onNext(NestResponse response) {
                handleResponse(sessionId, response, accumulator, finalTextConsumer, errorConsumer);
            }

            @Override
            public void onError(Throwable throwable) {
                log.warn("CLOVA STT 응답 스트림 오류가 발생했습니다. sessionId={}", sessionId, throwable);
                shutdownChannel(channel);
                if (terminated.compareAndSet(false, true)) {
                    errorConsumer.accept(throwable);
                }
            }

            @Override
            public void onCompleted() {
                log.info("CLOVA STT 응답 스트림이 종료되었습니다. sessionId={}", sessionId);
                shutdownChannel(channel);
            }
        });

        sendConfigRequest(sessionId, requestObserver, errorConsumer);
        return new DefaultClovaSpeechStream(sessionId, channel, requestObserver, errorConsumer, terminated);
    }

    // gRPC 서버 주소를 읽어 TLS 채널을 생성한다.
    private ManagedChannel buildChannel() {
        return NettyChannelBuilder.forTarget(normalizeGrpcTarget(clovaSttProperties.getGrpcUrl()))
                .useTransportSecurity()
                .build();
    }

    // Secret Key를 Authorization 헤더의 Bearer 토큰으로 넣는다.
    private Metadata buildMetadata() {
        Metadata metadata = new Metadata();
        metadata.put(
                Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
                "Bearer " + clovaSttProperties.getApiKey()
        );
        return metadata;
    }

    // 스트림 시작 직후 필수 Config JSON을 한 번 전송한다.
    private void sendConfigRequest(
            UUID sessionId,
            StreamObserver<NestRequest> requestObserver,
            Consumer<Throwable> errorConsumer
    ) {
        try {
            String configJson = objectMapper.writeValueAsString(new ConfigRequest(
                    new TranscriptionConfig(DEFAULT_LANGUAGE)
            ));

            requestObserver.onNext(NestRequest.newBuilder()
                    .setType(RequestType.CONFIG)
                    .setConfig(NestConfig.newBuilder()
                            .setConfig(configJson)
                            .build())
                    .build());
        } catch (Exception exception) {
            log.warn("CLOVA STT 설정 전송에 실패했습니다. sessionId={}", sessionId, exception);
            errorConsumer.accept(exception);
        }
    }

    // CLOVA 응답 contents JSON을 읽어 config/transcription/recognize 이벤트를 분기 처리한다.
    private void handleResponse(
            UUID sessionId,
            NestResponse response,
            ResponseAccumulator accumulator,
            Consumer<String> finalTextConsumer,
            Consumer<Throwable> errorConsumer
    ) {
        try {
            JsonNode root = objectMapper.readTree(response.getContents());
            log.info("CLOVA STT 응답 타입을 확인했습니다. sessionId={}, responseType={}", sessionId, root.path("responseType"));

            if (hasResponseType(root, "config")) {
                JsonNode configNode = root.path("config");
                String status = configNode.path("status").asText();
                log.info("CLOVA STT 설정 응답 상태입니다. sessionId={}, status={}", sessionId, status);
                if (!status.isBlank() && !"Success".equalsIgnoreCase(status)) {
                    errorConsumer.accept(new IllegalStateException("CLOVA STT 설정 응답이 실패했습니다: " + status));
                }
            }

            if (hasResponseType(root, "recognize")) {
                JsonNode recognizeNode = root.path("recognize");
                String status = recognizeNode.path("status").asText();
                log.info("CLOVA STT 인식 응답 상태입니다. sessionId={}, status={}", sessionId, status);
                if (!status.isBlank() && !"Success".equalsIgnoreCase(status)) {
                    errorConsumer.accept(new IllegalStateException("CLOVA STT 인식 응답이 실패했습니다: " + status));
                }
            }

            JsonNode transcriptionNode = root.path("transcription");
            if (!transcriptionNode.isMissingNode() && !transcriptionNode.isNull()) {
                String text = transcriptionNode.path("text").asText("");
                int position = transcriptionNode.path("position").asInt(0);
                boolean epFlag = transcriptionNode.path("epFlag").asBoolean(false);
                log.info(
                        "CLOVA STT 전사 응답을 파싱했습니다. sessionId={}, position={}, epFlag={}, textLength={}",
                        sessionId,
                        position,
                        epFlag,
                        text.length()
                );

                String fullText = accumulator.merge(text, position);
                if (epFlag) {
                    log.info("CLOVA STT 최종 텍스트를 반영했습니다. sessionId={}, textLength={}", sessionId, fullText.length());
                    finalTextConsumer.accept(fullText);
                } else {
                    log.info("CLOVA STT 중간 텍스트를 반영했습니다. sessionId={}, textLength={}", sessionId, fullText.length());
                    accumulator.partialTextConsumer.accept(fullText);
                }
            }
        } catch (Exception exception) {
            log.warn("CLOVA STT 응답 파싱에 실패했습니다. sessionId={}", sessionId, exception);
            errorConsumer.accept(exception);
        }
    }

    // 응답 타입 배열에 원하는 타입이 포함되어 있는지 확인한다.
    private boolean hasResponseType(JsonNode root, String expectedType) {
        JsonNode responseTypes = root.path("responseType");
        if (!responseTypes.isArray()) {
            return false;
        }

        Iterator<JsonNode> iterator = responseTypes.elements();
        while (iterator.hasNext()) {
            if (expectedType.equalsIgnoreCase(iterator.next().asText())) {
                return true;
            }
        }
        return false;
    }

    // gRPC URL이 URL이든 host:port 형태든 모두 forTarget에 맞게 정규화한다.
    private String normalizeGrpcTarget(String grpcUrl) {
        String normalized = grpcUrl.trim();
        normalized = normalized.replaceFirst("^https?://", "");
        normalized = normalized.replaceFirst("/+$", "");
        return normalized.toLowerCase(Locale.ROOT);
    }

    // 채널 종료 시 예외를 숨기고 조용히 처리한다.
    private void shutdownChannel(ManagedChannel channel) {
        channel.shutdownNow();
    }

    public interface ClovaSpeechStream {
        void sendAudio(byte[] audioBytes);

        void complete();

        void cancel(Throwable throwable);
    }

    // 내부 request observer에 DATA 요청을 전달하는 실제 스트림 구현체다.
    private static final class DefaultClovaSpeechStream implements ClovaSpeechStream {
        private final UUID sessionId;
        private final ManagedChannel channel;
        private final StreamObserver<NestRequest> requestObserver;
        private final Consumer<Throwable> errorConsumer;
        private final AtomicBoolean terminated;
        private final AtomicInteger sequenceId = new AtomicInteger(1);

        private DefaultClovaSpeechStream(
                UUID sessionId,
                ManagedChannel channel,
                StreamObserver<NestRequest> requestObserver,
                Consumer<Throwable> errorConsumer,
                AtomicBoolean terminated
        ) {
            this.sessionId = sessionId;
            this.channel = channel;
            this.requestObserver = requestObserver;
            this.errorConsumer = errorConsumer;
            this.terminated = terminated;
        }

        @Override
        public void sendAudio(byte[] audioBytes) {
            if (terminated.get() || audioBytes == null || audioBytes.length == 0) {
                return;
            }

            try {
                requestObserver.onNext(NestRequest.newBuilder()
                        .setType(RequestType.DATA)
                        .setData(NestData.newBuilder()
                                .setChunk(ByteString.copyFrom(audioBytes))
                                .build())
                        .build());
            } catch (Exception exception) {
                errorConsumer.accept(exception);
            }
        }

        @Override
        public void complete() {
            if (!terminated.compareAndSet(false, true)) {
                log.warn("이미 종료된 STT 스트림이라 complete 처리를 건너뜁니다. sessionId={}", sessionId);
                return;
            }

            try {
                String extraContents = "{\"epFlag\":true,\"seqId\":" + sequenceId.getAndIncrement() + "}";
                log.info("CLOVA STT 최종 epFlag 청크를 전송합니다. sessionId={}, extraContents={}", sessionId, extraContents);
                requestObserver.onNext(NestRequest.newBuilder()
                        .setType(RequestType.DATA)
                        .setData(NestData.newBuilder()
                                .setChunk(ByteString.EMPTY)
                                .setExtraContents(extraContents)
                                .build())
                        .build());
                log.info(
                        "CLOVA STT 최종 epFlag 청크 전송이 완료되었습니다. sessionId={}, onCompleted는 즉시 호출하지 않고 {}ms 동안 최종 응답을 기다립니다.",
                        sessionId,
                        FINAL_RESPONSE_WAIT_MS
                );
                CompletableFuture.delayedExecutor(FINAL_RESPONSE_WAIT_MS, TimeUnit.MILLISECONDS)
                        .execute(() -> log.info(
                                "CLOVA STT 최종 응답 대기 시간이 지났지만 자동 onCompleted는 호출하지 않았습니다. sessionId={}",
                                sessionId
                        ));
            } catch (Exception exception) {
                log.warn("CLOVA STT complete 처리에 실패했습니다. sessionId={}", sessionId, exception);
                errorConsumer.accept(exception);
                channel.shutdownNow();
            }
        }

        @Override
        public void cancel(Throwable throwable) {
            if (!terminated.compareAndSet(false, true)) {
                return;
            }

            try {
                requestObserver.onError(Status.CANCELLED
                        .withDescription("STT stream was cancelled. sessionId=" + sessionId)
                        .withCause(throwable)
                        .asRuntimeException());
            } finally {
                channel.shutdownNow();
            }
        }
    }

    // transcription 설정만 포함하는 최소 Config JSON 모델이다.
    private record ConfigRequest(TranscriptionConfig transcription) {
    }

    // 기본 언어 설정을 직렬화하기 위한 내부 모델이다.
    private record TranscriptionConfig(String language) {
    }

    // position 기반으로 partial/final full text를 조합한다.
    private static final class ResponseAccumulator {
        private final StringBuilder fullText = new StringBuilder();
        private final Consumer<String> partialTextConsumer;

        private ResponseAccumulator(Consumer<String> partialTextConsumer) {
            this.partialTextConsumer = partialTextConsumer;
        }

        private synchronized String merge(String text, int position) {
            int safePosition = Math.max(position, 0);
            while (fullText.length() < safePosition) {
                fullText.append(' ');
            }

            int end = safePosition + text.length();
            if (fullText.length() < end) {
                fullText.setLength(end);
            }

            fullText.replace(safePosition, end, text);
            return fullText.toString();
        }
    }
}
