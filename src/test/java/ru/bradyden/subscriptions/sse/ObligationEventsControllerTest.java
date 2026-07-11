package ru.bradyden.subscriptions.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class ObligationEventsControllerTest {
    @Test
    void subscribesWithFreshIdentifier() {
        var broadcaster = mock(SseBroadcaster.class);
        var emitter = mock(SseEmitter.class);
        var subscriberId = ArgumentCaptor.forClass(UUID.class);
        when(broadcaster.subscribe(subscriberId.capture())).thenReturn(emitter);
        var controller = new ObligationEventsController(broadcaster);

        assertThat(controller.events()).isSameAs(emitter);
        verify(broadcaster).subscribe(subscriberId.getValue());
        assertThat(subscriberId.getValue()).isNotNull();
    }
}
