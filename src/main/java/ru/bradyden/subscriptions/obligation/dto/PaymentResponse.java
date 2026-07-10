package ru.bradyden.subscriptions.obligation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id, UUID obligationId, BigDecimal amount, String currency, Instant paidAt) {}
