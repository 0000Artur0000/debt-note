package ru.bradyden.subscriptions.obligation;
import ru.bradyden.subscriptions.obligation.dto.*;
import ru.bradyden.subscriptions.payment.Payment;
import ru.bradyden.subscriptions.sse.SseBroadcaster;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static java.util.stream.Collectors.*;
@Service
public class ObligationService {
    private final ObligationRepository repo; private final EntityManager em;
    private final Clock chasy; private final SseBroadcaster sse;
    public ObligationService(ObligationRepository r, EntityManager e, Clock c, SseBroadcaster s) {
        repo=r; em=e; chasy=c; sse=s;
    }
    public CreateObligationResult sozdat(CreateObligationRequest z) {
        var t = LocalDate.now(chasy);
        var dub = repo.existsByNazvanieIgnoreCaseAndStatus(z.nazvanie(), Status.ACTIVE);
        var o = new Obligation();
        o.setNazvanie(z.nazvanie()); o.setSumma(z.summa()); o.setValuta(z.valuta());
        o.setKategoriya(z.kategoriya()); o.setPeriodichnost(z.periodichnost());
        o.setDataSledPlatezha(z.dataSledPlatezha());
        o.setStatus(z.dataSledPlatezha().isBefore(t) ? Status.EXPIRED : Status.ACTIVE);
        repo.save(o);
        return new CreateObligationResult(o, dub ? "Objazatelstvo s takim nazvaniem uzhe est" : null);
    }
    @Transactional
    public List<Obligation> poluchitSpisok(Category k, Status s) {
        var t = LocalDate.now(chasy); var n = Instant.now(chasy);
        repo.pogasitProsrochennye(Status.ACTIVE, Status.EXPIRED, t, n);
        var p = Obligation.builder().kategoriya(k).status(s).build();
        return repo.findAll(Example.of(p), Sort.by("dataSledPlatezha"));
    }
    public UpcomingResult blizhayshie(int d) {
        var t = LocalDate.now(chasy);
        var w = repo.findByDataSledPlatezhaBetweenOrderByDataSledPlatezhaAsc(t, t.plusDays(d));
        return w.stream().collect(teeing(
            toMap(Obligation::getValuta, Obligation::getSumma, BigDecimal::add),
            filtering(o->o.getKategoriya()==Category.SUBSCRIPTION&&o.getPeriodichnost()!=null,
                mapping(o->new UpcomingResult.RenewalAlert(o.getId(),o.getNazvanie(),
                    o.getSumma(),o.getValuta(),o.getDataSledPlatezha(),
                    o.getPeriodichnost().name().toLowerCase()), toList())),
            (tot,al)->new UpcomingResult(w,tot,al)));
    }
    @Transactional
    public PayResult oplatit(UUID id) {
        var o = repo.findById(id).orElseThrow();
        if (o.getStatus()!=Status.ACTIVE) throw new IllegalArgumentException("oplata tolko dlya aktivnyh");
        var p = Payment.builder().idObyazatelstva(o.getId()).summa(o.getSumma())
            .valuta(o.getValuta()).oplacheno(Instant.now(chasy)).build();
        em.persist(p);
        if (o.getPeriodichnost()==null) o.setStatus(Status.CANCELLED);
        else o.setDataSledPlatezha(o.getPeriodichnost().sleduyushchaya(o.getDataSledPlatezha()));
        return new PayResult(o, p);
    }
    @Transactional
    public void otmenit(UUID id) {
        var o = repo.findById(id).orElseThrow();
        if (o.getStatus()!=Status.ACTIVE) throw new IllegalArgumentException("otmena tolko dlya aktivnyh");
        o.setStatus(Status.CANCELLED);
    }
    @Transactional
    public void udalit(UUID id) {
        if (!repo.existsById(id)) throw new IllegalArgumentException("ne naydeno");
        repo.deleteById(id);
        sse.rasslat(new ObligationDeleted(id));
    }
}
