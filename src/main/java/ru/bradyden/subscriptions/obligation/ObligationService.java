package ru.bradyden.subscriptions.obligation;
import ru.bradyden.subscriptions.obligation.dto.*;
import ru.bradyden.subscriptions.payment.Payment;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static java.util.stream.Collectors.*;
@Service
public class ObligationService {
    private final ObligationRepository repo;
    private final EntityManager em;
    private final Clock chasy;
    public ObligationService(ObligationRepository repo, EntityManager em,
                             Clock chasy) {
        this.repo = repo; this.em = em; this.chasy = chasy;
    }
    public CreateObligationResult sozdat(CreateObligationRequest zapros) {
        var segodnya = LocalDate.now(chasy);
        var dublikat = repo.existsByNazvanieIgnoreCaseAndStatus(zapros.nazvanie(), Status.ACTIVE);
        var o = new Obligation();
        o.setNazvanie(zapros.nazvanie()); o.setSumma(zapros.summa());
        o.setValuta(zapros.valuta()); o.setKategoriya(zapros.kategoriya());
        o.setPeriodichnost(zapros.periodichnost());
        o.setDataSledPlatezha(zapros.dataSledPlatezha());
        o.setStatus(zapros.dataSledPlatezha().isBefore(segodnya) ? Status.EXPIRED : Status.ACTIVE);
        repo.save(o);
        var preduprezhdenie = dublikat ? "Objazatelstvo s takim nazvaniem uzhe est" : null;
        return new CreateObligationResult(o, preduprezhdenie);
    }
    @Transactional
    public List<Obligation> poluchitSpisok(Category k, Status s) {
        var segodnya = LocalDate.now(chasy); var seychas = Instant.now(chasy);
        repo.pogasitProsrochennye(Status.ACTIVE, Status.EXPIRED, segodnya, seychas);
        var proba = Obligation.builder().kategoriya(k).status(s).build();
        return repo.findAll(Example.of(proba), Sort.by("dataSledPlatezha"));
    }
    public UpcomingResult blizhayshie(int d) {
        var t = LocalDate.now(chasy);
        var w = repo.findByDataSledPlatezhaBetweenOrderByDataSledPlatezhaAsc(t, t.plusDays(d));
        return w.stream().collect(teeing(
            toMap(Obligation::getValuta, Obligation::getSumma, BigDecimal::add),
            filtering(o -> o.getKategoriya()==Category.SUBSCRIPTION && o.getPeriodichnost()!=null,
                mapping(o -> new UpcomingResult.RenewalAlert(o.getId(),o.getNazvanie(),
                    o.getSumma(),o.getValuta(),o.getDataSledPlatezha(),
                    o.getPeriodichnost().name().toLowerCase()), toList())),
            (tot, al) -> new UpcomingResult(w, tot, al)));
    }
    @Transactional
    public PayResult oplatit(UUID id) {
        var o = repo.findById(id).orElseThrow();
        if (o.getStatus() != Status.ACTIVE) throw new IllegalArgumentException("oplata tolko dlya aktivnyh");
        var p = Payment.builder().idObyazatelstva(o.getId()).summa(o.getSumma())
            .valuta(o.getValuta()).oplacheno(Instant.now(chasy)).build();
        em.persist(p);
        if (o.getPeriodichnost()==null) o.setStatus(Status.CANCELLED);
        else o.setDataSledPlatezha(o.getPeriodichnost().sleduyushchaya(o.getDataSledPlatezha()));
        return new PayResult(o, p);
    }
}
