package ru.bradyden.subscriptions.obligation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@Tag(name = "Obligations")
public class ObligationController {
    private final ObligationService service;
    private final SseBroadcaster sseBroadcaster;

    public ObligationController(ObligationService service, SseBroadcaster sseBroadcaster) {
        this.service = service;
        this.sseBroadcaster = sseBroadcaster;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать обязательство")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Обязательство создано",
                content =
                        @Content(schema = @Schema(implementation = CreateObligationResult.class))),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    })
    public CreateObligationResult create(@Valid @RequestBody CreateObligationRequest request) {
        return service.create(request);
    }

    @GetMapping
    @Operation(summary = "Получить обязательства")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Отфильтрованный список",
                content =
                        @Content(
                                array =
                                        @ArraySchema(
                                                schema =
                                                        @Schema(
                                                                implementation =
                                                                        ObligationResponse
                                                                                .class)))),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    })
    public List<ObligationResponse> list(
            @RequestParam(name = "category", required = false) Category category,
            @RequestParam(name = "status", required = false) Status status) {
        return service.list(category, status);
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Получить ближайшие платежи")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Платежи, totals и renewal alerts",
                content = @Content(schema = @Schema(implementation = UpcomingResult.class))),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    })
    public UpcomingResult upcoming(
            @RequestParam(name = "days", defaultValue = "7") @Min(0) int days) {
        return service.upcoming(days);
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Зафиксировать оплату")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Оплата зафиксирована",
                content = @Content(schema = @Schema(implementation = PayResult.class))),
        @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
        @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict"),
        @ApiResponse(responseCode = "422", ref = "#/components/responses/UnprocessableEntity")
    })
    public PayResult pay(@PathVariable UUID id) {
        return service.pay(id);
    }

    @PatchMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Отменить обязательство")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Обязательство отменено"),
        @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
        @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict"),
        @ApiResponse(responseCode = "422", ref = "#/components/responses/UnprocessableEntity")
    })
    public void cancel(@PathVariable UUID id) {
        service.cancel(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить обязательство")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Обязательство удалено"),
        @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
        @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    public void delete(@PathVariable UUID id) {
        service.delete(id);
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
        return sseBroadcaster.subscribe(UUID.randomUUID());
    }
}
