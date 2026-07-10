package ru.bradyden.subscriptions.obligation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.bradyden.subscriptions.obligation.Category;
import ru.bradyden.subscriptions.obligation.Obligation;
import ru.bradyden.subscriptions.obligation.Recurrence;
import ru.bradyden.subscriptions.obligation.Status;
import ru.bradyden.subscriptions.payment.Payment;

class ResponseMapperTest {
    private static final UUID OBLIGATION_ID =
            UUID.fromString("96bdd917-3fba-4976-a52f-5c1b8b8ba419");

    @Test
    void mapsEveryObligationField() {
        var createdAt = Instant.parse("2026-07-11T10:00:00Z");
        var updatedAt = Instant.parse("2026-07-11T11:00:00Z");
        var obligation = new Obligation();
        obligation.setId(OBLIGATION_ID);
        obligation.setTitle("Яндекс.Плюс");
        obligation.setAmount(new BigDecimal("399.00"));
        obligation.setCurrency("RUB");
        obligation.setCategory(Category.SUBSCRIPTION);
        obligation.setStatus(Status.ACTIVE);
        obligation.setRecurrence(Recurrence.MONTHLY);
        obligation.setNextPaymentDate(LocalDate.of(2026, 8, 9));
        obligation.setCreatedAt(createdAt);
        obligation.setUpdatedAt(updatedAt);

        var response = ObligationMapper.toResponse(obligation);

        assertThat(response)
                .isEqualTo(
                        new ObligationResponse(
                                OBLIGATION_ID,
                                "Яндекс.Плюс",
                                new BigDecimal("399.00"),
                                "RUB",
                                Category.SUBSCRIPTION,
                                Status.ACTIVE,
                                Recurrence.MONTHLY,
                                LocalDate.of(2026, 8, 9),
                                createdAt,
                                updatedAt));
    }

    @Test
    void mapsEveryPaymentField() {
        var paymentId = UUID.fromString("d40b9a30-288c-4880-927c-902f9ec84d4e");
        var paidAt = Instant.parse("2026-07-11T12:00:00Z");
        var payment = new Payment();
        payment.setId(paymentId);
        payment.setObligationId(OBLIGATION_ID);
        payment.setAmount(new BigDecimal("399.00"));
        payment.setCurrency("RUB");
        payment.setPaidAt(paidAt);

        var response = PaymentMapper.toResponse(payment);

        assertThat(response)
                .isEqualTo(
                        new PaymentResponse(
                                paymentId, OBLIGATION_ID, new BigDecimal("399.00"), "RUB", paidAt));
    }
}
