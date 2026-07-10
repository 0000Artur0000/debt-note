package ru.bradyden.subscriptions.obligation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import ru.bradyden.subscriptions.obligation.Category;
import ru.bradyden.subscriptions.obligation.Recurrence;

public record CreateObligationRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull @Positive BigDecimal amount,
        @NotNull String currency,
        @NotNull Category category,
        Recurrence recurrence,
        @NotNull LocalDate nextPaymentDate) {}
