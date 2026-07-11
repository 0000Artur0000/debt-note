package ru.bradyden.subscriptions.obligation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;
import ru.bradyden.subscriptions.payment.PaymentRepository;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:concurrent-payment;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.flyway.enabled=false",
            "spring.jpa.hibernate.ddl-auto=create-drop"
        })
class ConcurrentPaymentTest {
    @Autowired ObligationRepository obligationRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired Clock clock;
    @Autowired TransactionTemplate transactionTemplate;

    @Test
    void concurrentPaymentsKeepSingleStateChangeAndSinglePayment() throws Exception {
        var today = LocalDate.now(clock);
        var paymentDate = today.plusDays(10);
        var obligation =
                Obligation.create(
                        "Конкурентная подписка",
                        new BigDecimal("500.00"),
                        "RUB",
                        Category.SUBSCRIPTION,
                        Recurrence.MONTHLY,
                        paymentDate,
                        today);
        var obligationId = obligationRepository.saveAndFlush(obligation).getId();
        var bothLoaded = new CyclicBarrier(2);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> payAndCaptureFailure(obligationId, bothLoaded));
            var second = executor.submit(() -> payAndCaptureFailure(obligationId, bothLoaded));
            var outcomes =
                    List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));

            assertThat(outcomes)
                    .as("concurrent payment outcomes: %s", outcomes)
                    .filteredOn(Optional::isEmpty)
                    .hasSize(1);
            assertThat(outcomes.stream().flatMap(Optional::stream).toList())
                    .singleElement()
                    .isInstanceOf(OptimisticLockingFailureException.class);
        }

        var reloaded = obligationRepository.findById(obligationId).orElseThrow();
        assertThat(reloaded.getNextPaymentDate()).isEqualTo(paymentDate.plusMonths(1));
        assertThat(reloaded.getVersion()).isOne();
        assertThat(paymentRepository.count()).isOne();
    }

    private Optional<RuntimeException> payAndCaptureFailure(
            UUID obligationId, CyclicBarrier bothLoaded) {
        try {
            transactionTemplate.executeWithoutResult(
                    transaction -> {
                        var obligation = obligationRepository.findById(obligationId).orElseThrow();
                        await(bothLoaded);
                        var payment = obligation.pay(Instant.now(clock));
                        paymentRepository.save(payment);
                    });
            return Optional.empty();
        } catch (RuntimeException failure) {
            return Optional.of(failure);
        }
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while synchronizing transactions", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not synchronize transactions", exception);
        }
    }
}
