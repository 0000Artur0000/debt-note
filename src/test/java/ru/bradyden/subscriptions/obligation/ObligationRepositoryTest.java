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
        assertThat(versionOf(overdueOneOff)).isOne();
        assertThat(versionOf(overdueSubscription)).isZero();
        assertThat(versionOf(futureOneOff)).isZero();
        assertThat(versionOf(cancelledOneOff)).isZero();
    }

    @Test
    void lazyExpiryClearsStaleManagedEntitiesBeforeListQuery() {
        var id = persist(null, TODAY.minusDays(1), Status.ACTIVE);
        var managed = entityManager.find(Obligation.class, id);

        repository.expireOverdueOneOffs(
                Status.ACTIVE, Status.EXPIRED, TODAY, Instant.parse("2025-06-15T12:00:00Z"));

        assertThat(entityManager.getEntityManager().contains(managed)).isFalse();
        assertThat(repository.findAllFiltered(null, Status.EXPIRED))
                .singleElement()
                .extracting(Obligation::getId)
                .isEqualTo(id);
    }

    @Test
    void duplicateLookupIgnoresCaseAndMatchesStatus() {
        persist(Recurrence.MONTHLY, TODAY.plusDays(10), Status.ACTIVE, "Яндекс.Плюс");

        assertThat(repository.existsByTitleIgnoreCaseAndStatus("яндекс.плюс", Status.ACTIVE))
                .isTrue();
        assertThat(repository.existsByTitleIgnoreCaseAndStatus("яндекс.плюс", Status.CANCELLED))
                .isFalse();
    }

    @Test
    void persistenceInitializesOptimisticLockVersion() {
        var id = persist(Recurrence.MONTHLY, TODAY.plusDays(10), Status.ACTIVE);

        entityManager.clear();

        var obligation = entityManager.find(Obligation.class, id);
        assertThat(obligation.getVersion()).isZero();
        assertThat(obligation.getBillingAnchorDay()).isEqualTo((short) 25);
    }

    @Test
    void filteredListCombinesFiltersAndSortsByPaymentDate() {
        var later =
                persist(
                        Recurrence.MONTHLY,
                        TODAY.plusDays(10),
                        Status.ACTIVE,
                        "Поздняя подписка",
                        Category.SUBSCRIPTION);
        var earlier =
                persist(
                        Recurrence.MONTHLY,
                        TODAY.plusDays(2),
                        Status.ACTIVE,
                        "Ближайшая подписка",
                        Category.SUBSCRIPTION);
        persist(null, TODAY.plusDays(1), Status.ACTIVE, "Счёт", Category.BILL);
        persist(
                Recurrence.MONTHLY,
                TODAY.plusDays(3),
                Status.CANCELLED,
                "Отменённая подписка",
                Category.SUBSCRIPTION);

        var result = repository.findAllFiltered(Category.SUBSCRIPTION, Status.ACTIVE);

        assertThat(result).extracting(Obligation::getId).containsExactly(earlier, later);
    }

    @Test
    void upcomingIncludesOnlyActiveObligationsInsideInclusiveWindow() {
        var startsToday =
                persist(Recurrence.MONTHLY, TODAY, Status.ACTIVE, "Сегодня", Category.SUBSCRIPTION);
        var endsOnBoundary =
                persist(null, TODAY.plusDays(7), Status.ACTIVE, "На границе", Category.BILL);
        persist(
                Recurrence.MONTHLY,
                TODAY.plusDays(1),
                Status.CANCELLED,
                "Отменено",
                Category.SUBSCRIPTION);
        persist(null, TODAY.plusDays(2), Status.EXPIRED, "Истекло", Category.BILL);
        persist(null, TODAY.minusDays(1), Status.ACTIVE, "До окна", Category.BILL);
        persist(null, TODAY.plusDays(8), Status.ACTIVE, "После окна", Category.BILL);

        var result = repository.findUpcoming(Status.ACTIVE, TODAY, TODAY.plusDays(7));

        assertThat(result)
                .extracting(Obligation::getId)
                .containsExactly(startsToday, endsOnBoundary);
    }

    private UUID persist(Recurrence recurrence, LocalDate date, Status status) {
        return persist(recurrence, date, status, "Кинопоиск " + UUID.randomUUID());
    }

    private UUID persist(Recurrence recurrence, LocalDate date, Status status, String title) {
        return persist(recurrence, date, status, title, Category.SUBSCRIPTION);
    }

    private UUID persist(
            Recurrence recurrence, LocalDate date, Status status, String title, Category category) {
        var obligation =
                Obligation.create(
                        title,
                        new BigDecimal("399.00"),
                        "RUB",
                        category,
                        recurrence,
                        date,
                        status == Status.EXPIRED ? date.plusDays(1) : date.minusDays(1));
        if (status == Status.CANCELLED) {
            obligation.cancel();
        }
        return entityManager.persistAndFlush(obligation).getId();
    }

    private Status statusOf(UUID id) {
        entityManager.clear();
        return entityManager.find(Obligation.class, id).getStatus();
    }

    private long versionOf(UUID id) {
        entityManager.clear();
        return entityManager.find(Obligation.class, id).getVersion();
    }
}
