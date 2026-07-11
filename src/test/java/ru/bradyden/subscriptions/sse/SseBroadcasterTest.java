package ru.bradyden.subscriptions.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.bradyden.subscriptions.obligation.ObligationDeleted;

class SseBroadcasterTest {
    @Test
    void lifecycleCallbacksRemoveSubscriber() {
        var completionEmitter = mock(SseEmitter.class);
        var timeoutEmitter = mock(SseEmitter.class);
        var errorEmitter = mock(SseEmitter.class);
        var emitters = new ArrayDeque<SseEmitter>();
        emitters.add(completionEmitter);
        emitters.add(timeoutEmitter);
        emitters.add(errorEmitter);
        var broadcaster = new SseBroadcaster(emitters::remove);

        broadcaster.subscribe(UUID.randomUUID());
        broadcaster.subscribe(UUID.randomUUID());
        broadcaster.subscribe(UUID.randomUUID());

        callbackOf(completionEmitter).run();
        timeoutOf(timeoutEmitter).run();
        errorOf(errorEmitter).accept(new IOException("connection closed"));
        assertThat(broadcaster.subscriberCount()).isZero();
    }

    @Test
    void replacingSubscriberDoesNotLetOldCallbackRemoveNewEmitter() {
        var first = mock(SseEmitter.class);
        var second = mock(SseEmitter.class);
        var emitters = new ArrayDeque<SseEmitter>();
        emitters.add(first);
        emitters.add(second);
        var broadcaster = new SseBroadcaster(emitters::remove);
        var subscriberId = UUID.randomUUID();

        broadcaster.subscribe(subscriberId);
        broadcaster.subscribe(subscriberId);
        callbackOf(first).run();

        verify(first).complete();
        assertThat(broadcaster.subscriberCount()).isOne();
        callbackOf(second).run();
        assertThat(broadcaster.subscriberCount()).isZero();
    }

    @Test
    void deletionUsesContractEventName() throws Exception {
        var emitter = mock(SseEmitter.class);
        var broadcaster = new SseBroadcaster(() -> emitter);
        var event = new ObligationDeleted(UUID.randomUUID());
        broadcaster.subscribe(UUID.randomUUID());

        broadcaster.broadcast(event);

        var data = sentEvent(emitter).build().stream().map(item -> item.getData()).toList();
        assertThat(data).containsExactly("event:obligation_deleted\ndata:", event, "\n\n");
    }

    @Test
    void heartbeatUsesSseComment() throws Exception {
        var emitter = mock(SseEmitter.class);
        var broadcaster = new SseBroadcaster(() -> emitter);
        broadcaster.subscribe(UUID.randomUUID());

        broadcaster.heartbeat();

        var data = sentEvent(emitter).build().stream().map(item -> item.getData()).toList();
        assertThat(data).containsExactly(":heartbeat\n\n");
    }

    @Test
    void failedSendRemovesAndCompletesEmitter() throws Exception {
        var emitter = mock(SseEmitter.class);
        var broadcaster = new SseBroadcaster(() -> emitter);
        var failure = new IOException("broken pipe");
        doThrow(failure).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        broadcaster.subscribe(UUID.randomUUID());

        broadcaster.broadcast(new ObligationDeleted(UUID.randomUUID()));

        assertThat(broadcaster.subscriberCount()).isZero();
        verify(emitter).completeWithError(failure);
    }

    private static Runnable callbackOf(SseEmitter emitter) {
        var callback = ArgumentCaptor.forClass(Runnable.class);
        verify(emitter).onCompletion(callback.capture());
        return callback.getValue();
    }

    private static Runnable timeoutOf(SseEmitter emitter) {
        var callback = ArgumentCaptor.forClass(Runnable.class);
        verify(emitter).onTimeout(callback.capture());
        return callback.getValue();
    }

    @SuppressWarnings("unchecked")
    private static Consumer<Throwable> errorOf(SseEmitter emitter) {
        var callback = ArgumentCaptor.forClass(Consumer.class);
        verify(emitter).onError(callback.capture());
        return callback.getValue();
    }

    private static SseEmitter.SseEventBuilder sentEvent(SseEmitter emitter) throws Exception {
        var event = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(emitter).send(event.capture());
        return event.getValue();
    }
}
