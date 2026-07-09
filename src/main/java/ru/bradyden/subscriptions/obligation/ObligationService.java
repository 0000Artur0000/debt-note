package ru.bradyden.subscriptions.obligation;
import ru.bradyden.subscriptions.obligation.dto.*;
import ru.bradyden.subscriptions.payment.Payment;
import ru.bradyden.subscriptions.sse.SseBroadcaster;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
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
        var dub = repo.existsByTitleIgnoreCaseAndStatus(z.title(), Status.ACTIVE);
        var o = new Obligation();
        o.setTitle(z.title()); o.setAmount(z.amount()); o.setCurrency(z.currency());
        o.setCategory(z.category()); o.setRecurrence(z.recurrence());
        o.setNextPaymentDate(z.nextPaymentDate());
        o.setStatus(z.nextPaymentDate().isBefore(t) ? Status.EXPIRED : Status.ACTIVE);
        repo.save(o);
        return new CreateObligationResult(o, dub ? "Активное обязательство с таким названием уже существует" : null);
    }
    @Transactional
    public List<Obligation> poluchitSpisok(Category k, Status s) {
        var t = LocalDate.now(chasy); var n = Instant.now(chasy);
        repo.pogasitProsrochennye(Status.ACTIVE, Status.EXPIRED, t, n);
        var p = Obligation.builder().category(k).status(s).build();
        return repo.findAll(Example.of(p), Sort.by("nextPaymentDate"));
    }
    public UpcomingResult blizhayshie(int d) {
        var t = LocalDate.now(chasy);
        var w = repo.findByNextPaymentDateBetweenOrderByNextPaymentDateAsc(t, t.plusDays(d));
        return w.stream().collect(teeing(
            toMap(Obligation::getCurrency, Obligation::getAmount, BigDecimal::add),
            filtering(o->o.getCategory()==Category.SUBSCRIPTION&&o.getRecurrence()!=null,
                mapping(o->new UpcomingResult.RenewalAlert(o.getId(),o.getTitle(),
                    o.getAmount(),o.getCurrency(),o.getNextPaymentDate(),
                    o.getRecurrence().name().toLowerCase()), toList())),
            (tot,al)->new UpcomingResult(w,tot,al)));
    }
    @Transactional
    public PayResult oplatit(UUID id) {
        var o = najti(id);
        trebovatAktivny(o);
        var p = Payment.builder().obligationId(o.getId()).amount(o.getAmount())
            .currency(o.getCurrency()).paidAt(Instant.now(chasy)).build();
        em.persist(p);
        if (o.getRecurrence()==null) o.setStatus(Status.CANCELLED);
        else o.setNextPaymentDate(o.getRecurrence().sleduyushchaya(o.getNextPaymentDate()));
        return new PayResult(o, p);
    }
    @Transactional
    public void otmenit(UUID id) {
        var o = najti(id);
        trebovatAktivny(o);
        o.setStatus(Status.CANCELLED);
    }
    @Transactional
    public void udalit(UUID id) {
        if (!repo.existsById(id)) throw neNaydeno(id);
        repo.deleteById(id);
        sse.rasslat(new ObligationDeleted(id));
    }
    private Obligation najti(UUID id) {
        return repo.findById(id).orElseThrow(() -> neNaydeno(id));
    }
    private static void trebovatAktivny(Obligation o) {
        if (o.getStatus()!=Status.ACTIVE)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Действие доступно только для обязательства в статусе active, текущий статус: "
                    + o.getStatus().name().toLowerCase());
    }
    private static ResponseStatusException neNaydeno(UUID id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Обязательство не найдено: " + id);
    }
}
