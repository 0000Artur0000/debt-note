package ru.bradyden.subscriptions.obligation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest(
        properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class ObligationRepositoryTest {
    private static final LocalDate TODAY = LocalDate.of(2025, 6, 15);

    @Autowired ObligationRepository repository;
    @Autowired TestEntityManager entityManager;

    @Test
    void lazyExpiryExpiresOneOffsButKeepsRecurringObligationsActive() {
        var overdueOneOff = persist(null, TODAY.minusDays(1), Status.ACTIVE);
        var overdueSubscription = persist(Recurrence.MONTHLY, TODAY.minusDays(30), Status.ACTIVE);
        var futureOneOff = persist(null, TODAY.plusDays(1), Status.ACTIVE);
        var cancelledOneOff = persist(null, TODAY.minusDays(1), Status.CANCELLED);

        var updated =
                repository.expireOverdueOneOffs(
                        Status.ACTIVE, Status.EXPIRED, TODAY, Instant.now());

        assertThat(updated).isEqualTo(1);
        assertThat(statusOf(overdueOneOff)).isEqualTo(Status.EXPIRED);
        assertThat(statusOf(overdueSubscription)).isEqualTo(Status.ACTIVE);
        assertThat(statusOf(futureOneOff)).isEqualTo(Status.ACTIVE);
        assertThat(statusOf(cancelledOneOff)).isEqualTo(Status.CANCELLED);
    }

    @Test
    void duplicateLookupIgnoresCaseAndMatchesStatus() {
        persist(Recurrence.MONTHLY, TODAY.plusDays(10), Status.ACTIVE, "Яндекс.Плюс");

        assertThat(repository.existsByTitleIgnoreCaseAndStatus("яндекс.плюс", Status.ACTIVE))
                .isTrue();
        assertThat(repository.existsByTitleIgnoreCaseAndStatus("яндекс.плюс", Status.CANCELLED))
                .isFalse();
    }

    private UUID persist(Recurrence recurrence, LocalDate date, Status status) {
        return persist(recurrence, date, status, "Кинопоиск " + UUID.randomUUID());
    }

    private UUID persist(Recurrence recurrence, LocalDate date, Status status, String title) {
        var obligation = new Obligation();
        obligation.setTitle(title);
        obligation.setAmount(new BigDecimal("399.00"));
        obligation.setCurrency("RUB");
        obligation.setCategory(Category.SUBSCRIPTION);
        obligation.setRecurrence(recurrence);
        obligation.setNextPaymentDate(date);
        obligation.setStatus(status);
        return entityManager.persistAndFlush(obligation).getId();
    }

    private Status statusOf(UUID id) {
        entityManager.clear();
        return entityManager.find(Obligation.class, id).getStatus();
    }
}
