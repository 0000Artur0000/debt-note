package ru.bradyden.subscriptions.obligation;
import ru.bradyden.subscriptions.obligation.dto.CreateObligationRequest;
import ru.bradyden.subscriptions.obligation.dto.CreateObligationResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/obligations")
public class ObligationController {
    private final ObligationService servis;
    public ObligationController(ObligationService servis) {
        this.servis = servis;
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateObligationResult sozdat(
            @Valid @RequestBody CreateObligationRequest zapros) {
        return servis.sozdat(zapros);
    }
    @GetMapping
    public List<Obligation> spisok(
            @RequestParam(required = false) Category kategoriya,
            @RequestParam(required = false) Status status) {
        throw new UnsupportedOperationException("pokachto net");
    }
}
