package ru.bradyden.subscriptions.obligation;
import ru.bradyden.subscriptions.obligation.dto.*;
import ru.bradyden.subscriptions.payment.Payment;
import ru.bradyden.subscriptions.sse.SseBroadcaster;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class ObligationServiceTest {
    @Mock ObligationRepository repo; @Mock EntityManager em; @Mock SseBroadcaster sse;
    Clock chasy = Clock.fixed(Instant.parse("2026-07-09T12:00:00Z"), ZoneId.of("UTC"));
    ObligationService servis;
    @BeforeEach
    void setUp() {
        servis = new ObligationService(repo, em, chasy, sse);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    @Test void datumVProshlomSrazuExpired() {
        var z = new CreateObligationRequest("test",new BigDecimal("100"),"RUB",Category.SUBSCRIPTION,null,LocalDate.of(2026,1,1));
        assertThat(servis.sozdat(z).obligation().getStatus()).isEqualTo(Status.EXPIRED);
    }
    @Test void dublikatDaetPreduprezhdenie() {
        when(repo.existsByNazvanieIgnoreCaseAndStatus("test",Status.ACTIVE)).thenReturn(true);
        var z = new CreateObligationRequest("test",new BigDecimal("100"),"RUB",Category.SUBSCRIPTION,null,LocalDate.of(2026,8,1));
        assertThat(servis.sozdat(z).preduprezhdenie()).isNotNull();
    }
    @ParameterizedTest
    @CsvSource({"MONTHLY,2025-01-31,2025-02-28","MONTHLY,2024-01-31,2024-02-29","QUARTERLY,2025-01-15,2025-04-15","YEARLY,2024-02-29,2025-02-28"})
    void sdvigDaty(Recurrence r,LocalDate ot,LocalDate ozh){assertThat(r.sleduyushchaya(ot)).isEqualTo(ozh);}
    @Test void neAktivnyNelzyaOplatit(){
        var o=new Obligation();o.setId(UUID.randomUUID());o.setStatus(Status.CANCELLED);
        when(repo.findById(o.getId())).thenReturn(Optional.of(o));
        assertThatThrownBy(()->servis.oplatit(o.getId())).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void neAktivnyNelzyaOtmenit(){
        var o=new Obligation();o.setId(UUID.randomUUID());o.setStatus(Status.EXPIRED);
        when(repo.findById(o.getId())).thenReturn(Optional.of(o));
        assertThatThrownBy(()->servis.otmenit(o.getId())).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void oknoBezObyazatelstv(){
        when(repo.findByDataSledPlatezhaBetweenOrderByDataSledPlatezhaAsc(any(),any())).thenReturn(List.of());
        var r=servis.blizhayshie(7);
        assertThat(r.obligations()).isEmpty(); assertThat(r.totals()).isEmpty();
    }
}
