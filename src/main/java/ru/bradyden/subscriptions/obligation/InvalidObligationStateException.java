package ru.bradyden.subscriptions.obligation;

public final class InvalidObligationStateException extends RuntimeException {
    private final String operation;
    private final Status currentStatus;

    public InvalidObligationStateException(String operation, Status currentStatus) {
        super(
                "Операция '%s' доступна только для обязательства в статусе active, текущий статус: %s"
                        .formatted(operation, currentStatus.name().toLowerCase()));
        this.operation = operation;
        this.currentStatus = currentStatus;
    }

    public String getOperation() {
        return operation;
    }

    public Status getCurrentStatus() {
        return currentStatus;
    }
}
