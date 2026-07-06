package ru.bradyden.subscriptions.obligation;
import ru.bradyden.subscriptions.obligation.dto.CreateObligationRequest;
import ru.bradyden.subscriptions.obligation.dto.CreateObligationResult;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.LocalDate;
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
        return new CreateObligationResult(o, null);
    }
}
