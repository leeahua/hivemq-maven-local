package n1;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import v1.MessageQueueDrainedAckRequest;
import w.QueuedMessagesClusterPersistence;

public class MessageQueueDrainedAckRequestCallback extends ClusterCallback<Void, MessageQueueDrainedAckRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageQueueDrainedAckRequestCallback.class);
    private final QueuedMessagesClusterPersistence persistence;

    public MessageQueueDrainedAckRequestCallback(QueuedMessagesClusterPersistence persistence) {
        this.persistence = persistence;
    }

    public void onSuccess(@Nullable Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(this.persistence.ackDrained(this.request.getClientId()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.persistence.ackDrained(this.request.getClientId()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.persistence));
    }

    public void onTimedOut() {
        LOGGER.trace("Message queue drained ack timed out.");
        retryAndIncreaseTimeout(Void.class);
    }

    public void onBusy() {
        retry(Void.class);
    }

    private static class Task implements Runnable {
        private final SettableFuture<Void> settableFuture;
        private final MessageQueueDrainedAckRequest request;
        private final QueuedMessagesClusterPersistence persistence;

        Task(SettableFuture<Void> settableFuture,
             MessageQueueDrainedAckRequest request,
             QueuedMessagesClusterPersistence persistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.persistence = persistence;
        }

        public void run() {
            this.settableFuture.setFuture(this.persistence.ackDrained(this.request.getClientId()));
        }
    }
}
