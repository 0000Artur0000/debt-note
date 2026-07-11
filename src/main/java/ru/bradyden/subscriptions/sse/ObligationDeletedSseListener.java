package ru.bradyden.subscriptions.sse;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.bradyden.subscriptions.obligation.ObligationDeleted;

@Component
public class ObligationDeletedSseListener {
    private final SseBroadcaster broadcaster;

    public ObligationDeletedSseListener(SseBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void on(ObligationDeleted event) {
        broadcaster.broadcast(event);
    }
}
