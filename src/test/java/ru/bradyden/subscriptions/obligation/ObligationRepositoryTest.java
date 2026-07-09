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
@DataJpaTest(properties = {"spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop"})
class ObligationRepositoryTest {
    private static final LocalDate TODAY = LocalDate.of(2025,6,15);
    @Autowired ObligationRepository repository;
    @Autowired TestEntityManager entityManager;
    @Test void lazyExpiryGasitRazovyeNoOstavlyaetRekurrentnye() {
        var razovoeProsrocheno = persist(null, TODAY.minusDays(1), Status.ACTIVE);
        var podpiskaProsrochena = persist(Recurrence.MONTHLY, TODAY.minusDays(30), Status.ACTIVE);
        var razovoeBudushchee = persist(null, TODAY.plusDays(1), Status.ACTIVE);
        var uzheCancelled = persist(null, TODAY.minusDays(1), Status.CANCELLED);
        var obnovleno = repository.pogasitProsrochennye(Status.ACTIVE, Status.EXPIRED, TODAY, Instant.now());
        assertThat(obnovleno).isEqualTo(1);
        assertThat(statusOf(razovoeProsrocheno)).isEqualTo(Status.EXPIRED);
        assertThat(statusOf(podpiskaProsrochena)).isEqualTo(Status.ACTIVE);
        assertThat(statusOf(razovoeBudushchee)).isEqualTo(Status.ACTIVE);
        assertThat(statusOf(uzheCancelled)).isEqualTo(Status.CANCELLED);
    }
    @Test void proverkaDublyaBezRegistraIPoStatusu() {
        persist(Recurrence.MONTHLY, TODAY.plusDays(10), Status.ACTIVE, "Яндекс.Плюс");
        assertThat(repository.existsByTitleIgnoreCaseAndStatus("яндекс.плюс", Status.ACTIVE)).isTrue();
        assertThat(repository.existsByTitleIgnoreCaseAndStatus("яндекс.плюс", Status.CANCELLED)).isFalse();
    }
    private UUID persist(Recurrence r, LocalDate data, Status s) {
        return persist(r, data, s, "Кинопоиск " + UUID.randomUUID());
    }
    private UUID persist(Recurrence r, LocalDate data, Status s, String title) {
        var o = Obligation.builder().title(title).amount(new BigDecimal("399.00")).currency("RUB")
            .category(Category.SUBSCRIPTION).recurrence(r).nextPaymentDate(data).status(s).build();
        return entityManager.persistAndFlush(o).getId();
    }
    private Status statusOf(UUID id) {
        entityManager.clear();
        return entityManager.find(Obligation.class, id).getStatus();
    }
}
