package com.linker.relia.consultation.service.ai;

import com.linker.relia.consultation.dto.response.ConsultationAiBriefingSourceResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

final class ConsultationAiBriefingFingerprint {

    private ConsultationAiBriefingFingerprint() {
    }

    static String create(List<ConsultationAiBriefingSourceResponse> consultations) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            for (ConsultationAiBriefingSourceResponse consultation : consultations) {
                update(digest, consultation.getConsultationId());
                update(digest, consultation.getConsultationSequence());
                update(digest, consultation.getConsultedAt());
                update(digest, consultation.getConsultationType());
                update(digest, consultation.getConsultationChannel());
                update(digest, consultation.getFpName());
                update(digest, consultation.getSpecialNote());
                update(digest, consultation.getNextScheduledAt());
            }

            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private static void update(MessageDigest digest, Object value) {
        String text = value == null ? "<null>" : value.toString();
        digest.update(Integer.toString(text.length()).getBytes(StandardCharsets.UTF_8));
        digest.update((byte) ':');
        digest.update(text.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) '|');
    }
}
