package ru.bradyden.subscriptions.obligation;
import ru.bradyden.subscriptions.obligation.dto.*;
import ru.bradyden.subscriptions.payment.Payment;
import ru.bradyden.subscriptions.sse.SseBroadcaster;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class ObligationServiceTest {
    @Mock ObligationRepository repo;
    @Mock EntityManager em;
    @Mock SseBroadcaster sse;
    Clock chasy = Clock.fixed(Instant.parse("2026-07-09T12:00:00Z"), ZoneId.of("UTC"));
    ObligationService servis;
    @BeforeEach
    void setUp() {
        servis = new ObligationService(repo, em, chasy, sse);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    @Test
    void datumVProshlomSrazuExpired() {
        var z = new CreateObligationRequest("test", new BigDecimal("100"), "RUB",
            Category.SUBSCRIPTION, null, LocalDate.of(2026, 1, 1));
        var r = servis.sozdat(z);
        assertThat(r.obligation().getStatus()).isEqualTo(Status.EXPIRED);
    }
}
