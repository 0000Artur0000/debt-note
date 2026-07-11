package ru.bradyden.subscriptions.sse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.bradyden.subscriptions.obligation.ObligationDeleted;

@RestController
@RequestMapping("/obligations")
@Tag(name = "Obligations")
public class ObligationEventsController {
    private final SseBroadcaster broadcaster;

    public ObligationEventsController(SseBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @GetMapping("/events")
    @Operation(summary = "Подписаться на события удаления")
    @ApiResponse(
            responseCode = "200",
            description = "SSE stream",
            content =
                    @Content(
                            mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                            schema = @Schema(implementation = ObligationDeleted.class)))
    public SseEmitter events() {
        return broadcaster.subscribe(UUID.randomUUID());
    }
}
