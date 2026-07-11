package ru.bradyden.subscriptions.obligation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ObligationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 11);

    @Test
    void factoryDerivesStatusFromPaymentDate() {
        assertThat(create(null, TODAY).getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(create(null, TODAY.minusDays(1)).getStatus()).isEqualTo(Status.EXPIRED);
    }

    @Test
    void factoryRejectsInvalidCoreState() {
        assertThatThrownBy(
                        () ->
                                Obligation.create(
                                        " ",
                                        BigDecimal.ONE,
                                        "RUB",
                                        Category.BILL,
                                        null,
                                        TODAY,
                                        TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
        assertThatThrownBy(
                        () ->
                                Obligation.create(
                                        "Счёт",
                                        BigDecimal.ZERO,
                                        "RUB",
                                        Category.BILL,
                                        null,
                                        TODAY,
                                        TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void payingOneOffCreatesSnapshotAndCancelsObligation() {
        var obligation = create(null, TODAY.plusDays(1));
        var id = UUID.randomUUID();
        var paidAt = Instant.parse("2026-07-11T12:00:00Z");
        ReflectionTestUtils.setField(obligation, "id", id);

        var payment = obligation.pay(paidAt);

        assertThat(obligation.getStatus()).isEqualTo(Status.CANCELLED);
        assertThat(payment.getObligationId()).isEqualTo(id);
        assertThat(payment.getAmount()).isEqualByComparingTo("399.00");
        assertThat(payment.getCurrency()).isEqualTo("RUB");
        assertThat(payment.getPaidAt()).isEqualTo(paidAt);
    }

    @Test
    void payingRecurringObligationAdvancesDateAndKeepsItActive() {
        var obligation = create(Recurrence.MONTHLY, LocalDate.of(2026, 7, 31));

        obligation.pay(Instant.parse("2026-07-11T12:00:00Z"));

        assertThat(obligation.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(obligation.getNextPaymentDate()).isEqualTo(LocalDate.of(2026, 8, 31));
    }

    @Test
    void cancelChangesOnlyActiveObligation() {
        var obligation = create(null, TODAY.plusDays(1));

        obligation.cancel();

        assertThat(obligation.getStatus()).isEqualTo(Status.CANCELLED);
        assertThatThrownBy(obligation::cancel)
                .isInstanceOf(InvalidObligationStateException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void payRejectsExpiredObligation() {
        var obligation = create(null, TODAY.minusDays(1));

        assertThatThrownBy(() -> obligation.pay(Instant.parse("2026-07-11T12:00:00Z")))
                .isInstanceOf(InvalidObligationStateException.class)
                .hasMessageContaining("expired");
    }

    private static Obligation create(Recurrence recurrence, LocalDate nextPaymentDate) {
        return Obligation.create(
                "Подписка",
                new BigDecimal("399.00"),
                "RUB",
                Category.SUBSCRIPTION,
                recurrence,
                nextPaymentDate,
                TODAY);
    }
}
