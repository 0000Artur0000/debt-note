package ru.bradyden.subscriptions.obligation;
import ru.bradyden.subscriptions.obligation.dto.CreateObligationRequest;
import ru.bradyden.subscriptions.obligation.dto.CreateObligationResult;
import org.springframework.stereotype.Service;
@Service
public class ObligationService {
    private final ObligationRepository repo;
    public ObligationService(ObligationRepository repo) {
        this.repo = repo;
    }
    public CreateObligationResult sozdat(CreateObligationRequest zapros) {
        return null;
    }
}
