package ru.bradyden.subscriptions.sse;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.bradyden.subscriptions.obligation.ObligationDeleted;

@Component
public class SseBroadcaster {
    private static final Logger LOGGER = LoggerFactory.getLogger(SseBroadcaster.class);
    private static final long NO_TIMEOUT = Long.MAX_VALUE;

    private final ConcurrentHashMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Supplier<SseEmitter> emitterFactory;

    public SseBroadcaster() {
        this(() -> new SseEmitter(NO_TIMEOUT));
    }

    SseBroadcaster(Supplier<SseEmitter> emitterFactory) {
        this.emitterFactory = emitterFactory;
    }

    public SseEmitter subscribe(UUID subscriberId) {
        var emitter = emitterFactory.get();
        emitter.onCompletion(() -> emitters.remove(subscriberId, emitter));
        emitter.onTimeout(() -> emitters.remove(subscriberId, emitter));
        emitter.onError(error -> removeAfterError(subscriberId, emitter, error));

        var previous = emitters.put(subscriberId, emitter);
        if (previous != null) {
            previous.complete();
        }
        return emitter;
    }

    public void broadcast(ObligationDeleted event) {
        broadcast(() -> SseEmitter.event().name(event.type()).data(event));
    }

    @Scheduled(fixedDelayString = "${app.sse.heartbeat-interval-seconds:25}", timeUnit = SECONDS)
    void heartbeat() {
        broadcast(() -> SseEmitter.event().comment("heartbeat"));
    }

    int subscriberCount() {
        return emitters.size();
    }

    private void broadcast(Supplier<SseEmitter.SseEventBuilder> eventFactory) {
        emitters.forEach(
                (subscriberId, emitter) -> send(subscriberId, emitter, eventFactory.get()));
    }

    private void send(UUID subscriberId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException exception) {
            if (emitters.remove(subscriberId, emitter)) {
                LOGGER.debug("Removed disconnected SSE subscriber {}", subscriberId, exception);
                emitter.completeWithError(exception);
            }
        }
    }

    private void removeAfterError(UUID subscriberId, SseEmitter emitter, Throwable error) {
        if (emitters.remove(subscriberId, emitter)) {
            LOGGER.debug("Removed failed SSE subscriber {}", subscriberId, error);
        }
    }
}
