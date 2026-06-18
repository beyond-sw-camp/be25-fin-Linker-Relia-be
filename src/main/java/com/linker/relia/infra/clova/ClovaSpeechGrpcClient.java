package com.linker.relia.infra.clova;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Consumer;

// CLOVA 전용 gRPC 통신만 담당
@Component
public class ClovaSpeechGrpcClient {
    public ClovaSpeechStream openStream(
            UUID sessionId,
            Consumer<String> partialTextConsumer,
            Consumer<String> finalTextConsumer,
            Consumer<Throwable> errorConsumer
    ) {
        return new NoopClovaSpeechStream();
    }

    public interface ClovaSpeechStream {
        void sendAudio(byte[] audioBytes);

        void complete();

        void cancel(Throwable throwable);
    }

    private static final class NoopClovaSpeechStream implements ClovaSpeechStream {
        @Override
        public void sendAudio(byte[] audioBytes) {
        }

        @Override
        public void complete() {
        }

        @Override
        public void cancel(Throwable throwable) {
        }
    }
}
