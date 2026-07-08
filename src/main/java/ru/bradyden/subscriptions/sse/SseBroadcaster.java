package ru.bradyden.subscriptions.sse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.UUID;
@Component
public class SseBroadcaster {
    public SseEmitter podpisatsya(UUID id) { return null; }
}
