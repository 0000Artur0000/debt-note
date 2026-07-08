package ru.bradyden.subscriptions.obligation;
import ru.bradyden.subscriptions.obligation.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
        return servis.poluchitSpisok(kategoriya, status);
    }
    @GetMapping("/upcoming")
    public UpcomingResult blizhayshie(@RequestParam(defaultValue = "7")
            @Min(0) int days) {
        return servis.blizhayshie(days);
    }
}
