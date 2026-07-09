package ru.bradyden.subscriptions.obligation;
import ru.bradyden.subscriptions.obligation.dto.*;
import ru.bradyden.subscriptions.sse.SseBroadcaster;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.*;
@RestController @RequestMapping("/obligations")
public class ObligationController {
    private final ObligationService servis;
    private final SseBroadcaster sse;
    public ObligationController(ObligationService s, SseBroadcaster b) { servis=s; sse=b; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public CreateObligationResult sozdat(@Valid @RequestBody CreateObligationRequest z) { return servis.sozdat(z); }
    @GetMapping
    public List<Obligation> spisok(@RequestParam(name="category", required=false) Category k,
                                   @RequestParam(name="status", required=false) Status s) {
        return servis.poluchitSpisok(k, s);
    }
    @GetMapping("/upcoming")
    public UpcomingResult blizhayshie(@RequestParam(name="days", defaultValue="7") @Min(0) int d) { return servis.blizhayshie(d); }
    @PostMapping("/{id}/pay")
    public PayResult oplatit(@PathVariable UUID id) { return servis.oplatit(id); }
    @PatchMapping("/{id}/cancel") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void otmenit(@PathVariable UUID id) { servis.otmenit(id); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void udalit(@PathVariable UUID id) { servis.udalit(id); }
    @GetMapping("/events")
    public SseEmitter sobytiya() { return sse.podpisatsya(UUID.randomUUID()); }
}
