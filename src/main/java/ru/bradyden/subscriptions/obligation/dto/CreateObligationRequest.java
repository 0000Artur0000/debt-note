package ru.bradyden.subscriptions.obligation.dto;
import ru.bradyden.subscriptions.obligation.Category;
import ru.bradyden.subscriptions.obligation.Recurrence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
public record CreateObligationRequest(
    @NotBlank @Size(max = 255) String nazvanie,
    @NotNull @Positive BigDecimal summa,
    @NotNull String valuta,
    @NotNull Category kategoriya,
    Recurrence periodichnost,
    @NotNull LocalDate dataSledPlatezha
) {}
