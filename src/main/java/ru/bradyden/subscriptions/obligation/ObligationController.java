package ru.bradyden.subscriptions.obligation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.bradyden.subscriptions.obligation.dto.CreateObligationRequest;
import ru.bradyden.subscriptions.obligation.dto.CreateObligationResult;
import ru.bradyden.subscriptions.obligation.dto.ObligationResponse;
import ru.bradyden.subscriptions.obligation.dto.PayResult;
import ru.bradyden.subscriptions.obligation.dto.UpcomingResult;
import ru.bradyden.subscriptions.sse.SseBroadcaster;

@RestController
@RequestMapping("/obligations")
public class ObligationController {
    private final ObligationService service;
    private final SseBroadcaster sseBroadcaster;

    public ObligationController(ObligationService service, SseBroadcaster sseBroadcaster) {
        this.service = service;
        this.sseBroadcaster = sseBroadcaster;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateObligationResult create(@Valid @RequestBody CreateObligationRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<ObligationResponse> list(
            @RequestParam(name = "category", required = false) Category category,
            @RequestParam(name = "status", required = false) Status status) {
        return service.list(category, status);
    }

    @GetMapping("/upcoming")
    public UpcomingResult upcoming(
            @RequestParam(name = "days", defaultValue = "7") @Min(0) int days) {
        return service.upcoming(days);
    }

    @PostMapping("/{id}/pay")
    public PayResult pay(@PathVariable UUID id) {
        return service.pay(id);
    }

    @PatchMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id) {
        service.cancel(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @GetMapping("/events")
    public SseEmitter events() {
        return sseBroadcaster.subscribe(UUID.randomUUID());
    }
}
