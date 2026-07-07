package ru.bradyden.subscriptions.obligation;
import ru.bradyden.subscriptions.obligation.dto.CreateObligationRequest;
import ru.bradyden.subscriptions.obligation.dto.CreateObligationResult;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
@Service
public class ObligationService {
    private final ObligationRepository repo;
    private final Clock chasy;
    public ObligationService(ObligationRepository repo, Clock chasy) {
        this.repo = repo;
        this.chasy = chasy;
    }
    public CreateObligationResult sozdat(CreateObligationRequest zapros) {
        var segodnya = LocalDate.now(chasy);
        var dublikat = repo.existsByNazvanieIgnoreCaseAndStatus(
            zapros.nazvanie(), Status.ACTIVE);
        var o = new Obligation();
        o.setNazvanie(zapros.nazvanie());
        o.setSumma(zapros.summa());
        o.setValuta(zapros.valuta());
        o.setKategoriya(zapros.kategoriya());
        o.setPeriodichnost(zapros.periodichnost());
        o.setDataSledPlatezha(zapros.dataSledPlatezha());
        o.setStatus(zapros.dataSledPlatezha().isBefore(segodnya)
            ? Status.EXPIRED : Status.ACTIVE);
        repo.save(o);
        var preduprezhdenie = dublikat
            ? "Objazatelstvo s takim nazvaniem uzhe est" : null;
        return new CreateObligationResult(o, preduprezhdenie);
    }
    public List<Obligation> poluchitSpisok(Category kategoriya, Status status) {
        var proba = Obligation.builder()
            .kategoriya(kategoriya)
            .status(status)
            .build();
        return repo.findAll(Example.of(proba),
            Sort.by("dataSledPlatezha"));
    }
}
