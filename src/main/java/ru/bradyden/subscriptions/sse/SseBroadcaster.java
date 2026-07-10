package ru.bradyden.subscriptions.sse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseBroadcaster {
    private final ConcurrentHashMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID subscriberId) {
        var emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(subscriberId, emitter);
        emitter.onCompletion(() -> emitters.remove(subscriberId));
        emitter.onTimeout(() -> emitters.remove(subscriberId));
        return emitter;
    }

    public void broadcast(Object event) {
        emitters.values().forEach(emitter -> send(emitter, event));
    }

    private static void send(SseEmitter emitter, Object event) {
        try {
            emitter.send(SseEmitter.event().name("sobytie").data(event));
        } catch (IOException ignored) {
            // Connection cleanup and logging are added in the dedicated SSE stage.
        }
    }
}
