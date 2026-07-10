package ru.bradyden.subscriptions.obligation;

import java.util.UUID;

public final class ObligationNotFoundException extends RuntimeException {
    private final UUID obligationId;

    public ObligationNotFoundException(UUID obligationId) {
        super("Обязательство не найдено: " + obligationId);
        this.obligationId = obligationId;
    }

    public UUID getObligationId() {
        return obligationId;
    }
}
