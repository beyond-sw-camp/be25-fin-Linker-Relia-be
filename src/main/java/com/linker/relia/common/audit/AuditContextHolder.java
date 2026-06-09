package com.linker.relia.common.audit;

import java.util.Optional;
import java.util.UUID;

public final class AuditContextHolder {
    private static final ThreadLocal<UUID> CURRENT_AUDITOR = new ThreadLocal<>();

    private AuditContextHolder() {
    }

    public static Optional<UUID> getCurrentAuditor() {
        return Optional.ofNullable(CURRENT_AUDITOR.get());
    }

    public static void setCurrentAuditor(UUID auditorId) {
        CURRENT_AUDITOR.set(auditorId);
    }

    public static void clear() {
        CURRENT_AUDITOR.remove();
    }
}
