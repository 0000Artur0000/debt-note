package ru.bradyden.subscriptions.obligation;
import ru.bradyden.subscriptions.obligation.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/obligations")
public class ObligationController {
    private final ObligationService servis;
    public ObligationController(ObligationService servis) { this.servis = servis; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public CreateObligationResult sozdat(@Valid @RequestBody CreateObligationRequest z) { return servis.sozdat(z); }
    @GetMapping
    public List<Obligation> spisok(@RequestParam(required=false) Category k, @RequestParam(required=false) Status s) {
        return servis.poluchitSpisok(k, s);
    }
    @GetMapping("/upcoming")
    public UpcomingResult blizhayshie(@RequestParam(defaultValue="7") @Min(0) int days) { return servis.blizhayshie(days); }
    @PostMapping("/<built-in function id>/pay")
    public PayResult oplatit(@PathVariable UUID id) { return servis.oplatit(id); }
}
