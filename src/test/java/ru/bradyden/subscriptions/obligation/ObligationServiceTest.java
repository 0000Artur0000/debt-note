package ru.bradyden.subscriptions.obligation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.bradyden.subscriptions.obligation.dto.CreateObligationRequest;
import ru.bradyden.subscriptions.payment.Payment;
import ru.bradyden.subscriptions.payment.PaymentRepository;
import ru.bradyden.subscriptions.sse.SseBroadcaster;

@ExtendWith(MockitoExtension.class)
class ObligationServiceTest {
    @Mock ObligationRepository repository;
    @Mock PaymentRepository paymentRepository;
    @Mock SseBroadcaster sseBroadcaster;

    private final Clock clock =
            Clock.fixed(Instant.parse("2026-07-09T12:00:00Z"), ZoneId.of("UTC"));

    private ObligationService service;

    @BeforeEach
    void setUp() {
        service = new ObligationService(repository, paymentRepository, clock, sseBroadcaster);
        lenient().when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void creationWithPastDateProducesExpiredObligation() {
        var request = request("test", null, LocalDate.of(2026, 1, 1));

        assertThat(service.create(request).obligation().status()).isEqualTo(Status.EXPIRED);
    }

    @Test
    void activeDuplicateProducesWarning() {
        when(repository.existsByTitleIgnoreCaseAndStatus("test", Status.ACTIVE)).thenReturn(true);
        var request = request("test", null, LocalDate.of(2026, 8, 1));

        assertThat(service.create(request).warning()).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({
        "MONTHLY,2025-01-31,2025-02-28",
        "MONTHLY,2024-01-31,2024-02-29",
        "QUARTERLY,2025-01-15,2025-04-15",
        "YEARLY,2024-02-29,2025-02-28"
    })
    void recurrenceCalculatesNextDate(
            Recurrence recurrence, LocalDate currentDate, LocalDate expectedDate) {
        assertThat(recurrence.nextDate(currentDate)).isEqualTo(expectedDate);
    }

    @ParameterizedTest
    @CsvSource({
        "MONTHLY,2026-08-15,2026-09-15",
        "QUARTERLY,2026-08-15,2026-11-15",
        "YEARLY,2026-08-15,2027-08-15"
    })
    void payingRecurringObligationAdvancesDateAndKeepsItActive(
            Recurrence recurrence, LocalDate currentDate, LocalDate nextDate) {
        var obligation = activeObligation(recurrence, currentDate);
        when(repository.findById(obligation.getId())).thenReturn(Optional.of(obligation));

        var result = service.pay(obligation.getId());

        assertThat(result.obligation().status()).isEqualTo(Status.ACTIVE);
        assertThat(result.obligation().nextPaymentDate()).isEqualTo(nextDate);
        assertThat(result.payment()).isNotNull();
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void payingMonthlyObligationOnJanuary31SelectsFebruary28() {
        var obligation = activeObligation(Recurrence.MONTHLY, LocalDate.of(2026, 1, 31));
        when(repository.findById(obligation.getId())).thenReturn(Optional.of(obligation));

        assertThat(service.pay(obligation.getId()).obligation().nextPaymentDate())
                .isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void payingOneOffObligationCancelsIt() {
        var obligation = activeObligation(null, LocalDate.of(2026, 8, 1));
        when(repository.findById(obligation.getId())).thenReturn(Optional.of(obligation));

        var result = service.pay(obligation.getId());

        assertThat(result.obligation().status()).isEqualTo(Status.CANCELLED);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void payingInactiveObligationReturns422() {
        var obligation = activeObligation(null, LocalDate.of(2026, 8, 1));
        obligation.cancel();
        when(repository.findById(obligation.getId())).thenReturn(Optional.of(obligation));

        assertThatThrownBy(() -> service.pay(obligation.getId()))
                .isInstanceOf(InvalidObligationStateException.class)
                .hasMessageContaining("pay")
                .hasMessageContaining("cancelled");
    }

    @Test
    void cancellingInactiveObligationReturns422() {
        var obligation = expiredObligation(LocalDate.of(2026, 8, 1));
        when(repository.findById(obligation.getId())).thenReturn(Optional.of(obligation));

        assertThatThrownBy(() -> service.cancel(obligation.getId()))
                .isInstanceOf(InvalidObligationStateException.class)
                .hasMessageContaining("cancel")
                .hasMessageContaining("expired");
    }

    @Test
    void deletingUnknownObligationReturns404() {
        var id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ObligationNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void listAppliesLazyExpiryBeforeQuerying() {
        when(repository.findAllFiltered(null, null)).thenReturn(List.of());

        service.list(null, null);

        var ordered = inOrder(repository);
        ordered.verify(repository)
                .expireOverdueOneOffs(eq(Status.ACTIVE), eq(Status.EXPIRED), any(), any());
        ordered.verify(repository).findAllFiltered(null, null);
    }

    @Test
    void upcomingWindowCanBeEmpty() {
        when(repository.findByNextPaymentDateBetweenOrderByNextPaymentDateAsc(any(), any()))
                .thenReturn(List.of());

        var result = service.upcoming(7);

        assertThat(result.obligations()).isEmpty();
        assertThat(result.totals()).isEmpty();
    }

    private static CreateObligationRequest request(
            String title, Recurrence recurrence, LocalDate nextPaymentDate) {
        return new CreateObligationRequest(
                title,
                new BigDecimal("100"),
                "RUB",
                Category.SUBSCRIPTION,
                recurrence,
                nextPaymentDate);
    }

    private static Obligation activeObligation(Recurrence recurrence, LocalDate nextPaymentDate) {
        var obligation =
                Obligation.create(
                        "test",
                        new BigDecimal("100"),
                        "RUB",
                        Category.SUBSCRIPTION,
                        recurrence,
                        nextPaymentDate,
                        nextPaymentDate.minusDays(1));
        ReflectionTestUtils.setField(obligation, "id", UUID.randomUUID());
        return obligation;
    }

    private static Obligation expiredObligation(LocalDate nextPaymentDate) {
        var obligation =
                Obligation.create(
                        "test",
                        new BigDecimal("100"),
                        "RUB",
                        Category.SUBSCRIPTION,
                        null,
                        nextPaymentDate,
                        nextPaymentDate.plusDays(1));
        ReflectionTestUtils.setField(obligation, "id", UUID.randomUUID());
        return obligation;
    }
}
