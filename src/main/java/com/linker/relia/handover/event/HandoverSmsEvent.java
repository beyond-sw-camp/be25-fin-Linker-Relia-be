package com.linker.relia.handover.event;

public record HandoverSmsEvent(
        String customerPhone,
        String newFpName
) {
}
