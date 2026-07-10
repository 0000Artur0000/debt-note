package ru.bradyden.subscriptions.obligation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ru.bradyden.subscriptions.obligation.Obligation;

public record UpcomingResult(
        List<Obligation> obligations,
        Map<String, BigDecimal> totals,
        List<RenewalAlert> renewalAlerts) {
    public record RenewalAlert(
            UUID id,
            String title,
            BigDecimal amount,
            String currency,
            LocalDate nextPaymentDate,
            String recurrence) {}
}
