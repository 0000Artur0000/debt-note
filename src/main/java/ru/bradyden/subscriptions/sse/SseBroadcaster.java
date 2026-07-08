package ru.bradyden.subscriptions.sse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
@Component
public class SseBroadcaster {
    private final ConcurrentHashMap<UUID, SseEmitter> palaty = new ConcurrentHashMap<>();
    public SseEmitter podpisatsya(UUID id) {
        var e = new SseEmitter(Long.MAX_VALUE);
        palaty.put(id, e);
        e.onCompletion(() -> palaty.remove(id));
        e.onTimeout(() -> palaty.remove(id));
        return e;
    }
    public void rasslat(Object s) {
        palaty.values().forEach(em -> { try { em.send(SseEmitter.event().name("sobytie").data(s)); } catch (IOException ex) {} });
    }
}
