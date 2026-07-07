package ru.bradyden.subscriptions.obligation.dto;
import ru.bradyden.subscriptions.obligation.Category;
import ru.bradyden.subscriptions.obligation.Recurrence;
import java.math.BigDecimal;
import java.time.LocalDate;
public record CreateObligationRequest(
    String nazvanie,
    BigDecimal summa,
    String valuta,
    Category kategoriya,
    Recurrence periodichnost,
    LocalDate dataSledPlatezha
) {}
