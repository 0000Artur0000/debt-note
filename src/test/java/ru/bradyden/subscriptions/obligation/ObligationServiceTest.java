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
import org.springframework.data.domain.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class ObligationServiceTest {
    @Mock ObligationRepository repo; @Mock EntityManager em; @Mock SseBroadcaster sse;
    Clock chasy = Clock.fixed(Instant.parse("2026-07-09T12:00:00Z"), ZoneId.of("UTC"));
    ObligationService servis;
    @BeforeEach
    void setUp() {
        servis = new ObligationService(repo, em, chasy, sse);
        lenient().when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    @Test void datumVProshlomSrazuExpired() {
        var z = new CreateObligationRequest("test",new BigDecimal("100"),"RUB",Category.SUBSCRIPTION,null,LocalDate.of(2026,1,1));
        assertThat(servis.sozdat(z).obligation().getStatus()).isEqualTo(Status.EXPIRED);
    }
    @Test void dublikatDaetPreduprezhdenie() {
        when(repo.existsByTitleIgnoreCaseAndStatus("test",Status.ACTIVE)).thenReturn(true);
        var z = new CreateObligationRequest("test",new BigDecimal("100"),"RUB",Category.SUBSCRIPTION,null,LocalDate.of(2026,8,1));
        assertThat(servis.sozdat(z).warning()).isNotNull();
    }
    @ParameterizedTest
    @CsvSource({"MONTHLY,2025-01-31,2025-02-28","MONTHLY,2024-01-31,2024-02-29","QUARTERLY,2025-01-15,2025-04-15","YEARLY,2024-02-29,2025-02-28"})
    void sdvigDaty(Recurrence r,LocalDate ot,LocalDate ozh){assertThat(r.sleduyushchaya(ot)).isEqualTo(ozh);}
    @ParameterizedTest
    @CsvSource({"MONTHLY,2026-08-15,2026-09-15","QUARTERLY,2026-08-15,2026-11-15","YEARLY,2026-08-15,2027-08-15"})
    void oplataRekurrentnojSdvigaetDatuIOstavlyaetActive(Recurrence r,LocalDate cur,LocalDate next){
        var o=aktivnoe(r,cur);
        when(repo.findById(o.getId())).thenReturn(Optional.of(o));
        var res=servis.oplatit(o.getId());
        assertThat(res.obligation().getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(res.obligation().getNextPaymentDate()).isEqualTo(next);
        assertThat(res.payment()).isNotNull();
        verify(em).persist(any(Payment.class));
    }
    @Test void oplata31ChislaMonthlyDaet28Fevralya(){
        var o=aktivnoe(Recurrence.MONTHLY,LocalDate.of(2026,1,31));
        when(repo.findById(o.getId())).thenReturn(Optional.of(o));
        assertThat(servis.oplatit(o.getId()).obligation().getNextPaymentDate()).isEqualTo(LocalDate.of(2026,2,28));
    }
    @Test void oplataRazovogoZakryvaetObyazatelstvo(){
        var o=aktivnoe(null,LocalDate.of(2026,8,1));
        when(repo.findById(o.getId())).thenReturn(Optional.of(o));
        var res=servis.oplatit(o.getId());
        assertThat(res.obligation().getStatus()).isEqualTo(Status.CANCELLED);
        verify(em).persist(any(Payment.class));
    }
    @Test void neAktivnyNelzyaOplatit(){
        var o=aktivnoe(null,LocalDate.of(2026,8,1));o.setStatus(Status.CANCELLED);
        when(repo.findById(o.getId())).thenReturn(Optional.of(o));
        assertThatThrownBy(()->servis.oplatit(o.getId()))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e->assertThat(((ResponseStatusException)e).getStatusCode().value()).isEqualTo(422));
    }
    @Test void neAktivnyNelzyaOtmenit(){
        var o=aktivnoe(null,LocalDate.of(2026,8,1));o.setStatus(Status.EXPIRED);
        when(repo.findById(o.getId())).thenReturn(Optional.of(o));
        assertThatThrownBy(()->servis.otmenit(o.getId()))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e->assertThat(((ResponseStatusException)e).getStatusCode().value()).isEqualTo(422));
    }
    @Test void udalenieNesushchestvuyushchego404(){
        var id=UUID.randomUUID();
        when(repo.existsById(id)).thenReturn(false);
        assertThatThrownBy(()->servis.udalit(id))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e->assertThat(((ResponseStatusException)e).getStatusCode().value()).isEqualTo(404));
    }
    @Test void spisokPrimenyaetLazyExpiryPeredVyborkoj(){
        when(repo.findAll(any(Example.class),any(Sort.class))).thenReturn(List.of());
        servis.poluchitSpisok(null,null);
        var poryadok=inOrder(repo);
        poryadok.verify(repo).pogasitProsrochennye(eq(Status.ACTIVE),eq(Status.EXPIRED),any(),any());
        poryadok.verify(repo).findAll(any(Example.class),any(Sort.class));
    }
    @Test void oknoBezObyazatelstv(){
        when(repo.findByNextPaymentDateBetweenOrderByNextPaymentDateAsc(any(),any())).thenReturn(List.of());
        var r=servis.blizhayshie(7);
        assertThat(r.obligations()).isEmpty(); assertThat(r.totals()).isEmpty();
    }
    private Obligation aktivnoe(Recurrence r,LocalDate data){
        var o=new Obligation();
        o.setId(UUID.randomUUID());
        o.setTitle("t");o.setAmount(new BigDecimal("100"));o.setCurrency("RUB");
        o.setCategory(Category.SUBSCRIPTION);o.setRecurrence(r);
        o.setNextPaymentDate(data);o.setStatus(Status.ACTIVE);
        return o;
    }
}
