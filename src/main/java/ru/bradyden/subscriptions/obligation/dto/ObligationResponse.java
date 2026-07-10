package ru.bradyden.subscriptions.obligation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import ru.bradyden.subscriptions.obligation.Category;
import ru.bradyden.subscriptions.obligation.Recurrence;
import ru.bradyden.subscriptions.obligation.Status;

public record ObligationResponse(
        UUID id,
        String title,
        BigDecimal amount,
        String currency,
        Category category,
        Status status,
        Recurrence recurrence,
        LocalDate nextPaymentDate,
        Instant createdAt,
        Instant updatedAt) {}
