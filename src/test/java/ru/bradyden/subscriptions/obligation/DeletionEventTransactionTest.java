package ru.bradyden.subscriptions.obligation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import ru.bradyden.subscriptions.sse.SseBroadcaster;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:deletion-event;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.flyway.enabled=false",
            "spring.jpa.hibernate.ddl-auto=create-drop"
        })
class DeletionEventTransactionTest {
    @Autowired ObligationService service;
    @Autowired ObligationRepository repository;
    @Autowired TransactionTemplate transactionTemplate;
    @Autowired Clock clock;

    @MockitoBean SseBroadcaster broadcaster;

    @Test
    void committedDeletionBroadcastsAfterCommit() {
        var id = persistObligation();

        service.delete(id);

        assertThat(repository.existsById(id)).isFalse();
        verify(broadcaster).broadcast(new ObligationDeleted(id));
    }

    @Test
    void rolledBackDeletionDoesNotBroadcast() {
        var id = persistObligation();

        transactionTemplate.executeWithoutResult(
                transaction -> {
                    service.delete(id);
                    transaction.setRollbackOnly();
                });

        assertThat(repository.existsById(id)).isTrue();
        verifyNoInteractions(broadcaster);
    }

    private UUID persistObligation() {
        var today = LocalDate.now(clock);
        var obligation =
                Obligation.create(
                        "Удаляемое обязательство",
                        new BigDecimal("100.00"),
                        "RUB",
                        Category.BILL,
                        null,
                        today.plusDays(1),
                        today);
        return repository.saveAndFlush(obligation).getId();
    }
}
