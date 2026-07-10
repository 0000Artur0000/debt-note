package ru.bradyden.subscriptions.obligation.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import ru.bradyden.subscriptions.common.validation.IsoCurrency;
import ru.bradyden.subscriptions.obligation.Category;
import ru.bradyden.subscriptions.obligation.Recurrence;

public record CreateObligationRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotBlank @IsoCurrency String currency,
        @NotNull Category category,
        Recurrence recurrence,
        @NotNull LocalDate nextPaymentDate) {
    public CreateObligationRequest {
        if (title != null) {
            title = title.strip();
        }
        if (currency != null) {
            currency = currency.strip().toUpperCase(Locale.ROOT);
        }
    }
}
