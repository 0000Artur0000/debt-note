package ru.bradyden.subscriptions.obligation;
import java.util.UUID;
public record ObligationDeleted(String type, UUID id) {
    public static final String TIP = "obligation_deleted";
    public ObligationDeleted(UUID id) { this(TIP, id); }
}
