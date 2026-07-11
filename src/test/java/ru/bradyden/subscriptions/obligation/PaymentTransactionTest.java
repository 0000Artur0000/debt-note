package ru.bradyden.subscriptions.obligation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.bradyden.subscriptions.payment.Payment;
import ru.bradyden.subscriptions.payment.PaymentRepository;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:payment-transaction;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.flyway.enabled=false",
            "spring.jpa.hibernate.ddl-auto=create-drop"
        })
class PaymentTransactionTest {
    @Autowired ObligationService service;
    @Autowired ObligationRepository obligationRepository;
    @Autowired Clock clock;

    @MockitoBean PaymentRepository paymentRepository;

    @Test
    void paymentPersistenceFailureRollsBackObligationChange() {
        var today = LocalDate.now(clock);
        var paymentDate = today.plusDays(10);
        var obligation =
                Obligation.create(
                        "Разовый счёт",
                        new BigDecimal("500.00"),
                        "RUB",
                        Category.BILL,
                        null,
                        paymentDate,
                        today);
        var obligationId = obligationRepository.saveAndFlush(obligation).getId();
        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(new DataIntegrityViolationException("payment insert failed"));

        assertThatThrownBy(() -> service.pay(obligationId))
                .isInstanceOf(DataIntegrityViolationException.class);

        var reloaded = obligationRepository.findById(obligationId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(reloaded.getNextPaymentDate()).isEqualTo(paymentDate);
    }
}
