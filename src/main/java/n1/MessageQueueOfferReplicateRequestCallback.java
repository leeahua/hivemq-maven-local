package n1;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import v1.MessageQueueOfferReplicateRequest;
import w.QueuedMessagesClusterPersistence;

public class MessageQueueOfferReplicateRequestCallback
        extends ClusterCallback<Void, MessageQueueOfferReplicateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageQueueOfferReplicateRequestCallback.class);
    private final QueuedMessagesClusterPersistence persistence;

    public MessageQueueOfferReplicateRequestCallback(QueuedMessagesClusterPersistence persistence) {
        this.persistence = persistence;
    }

    public void onSuccess(@Nullable Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(this.persistence.replicateOffer(this.request.getClientId(), this.request.getPublish(), this.request.getTimestamp(), this.request.getVectorClock()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.persistence.replicateOffer(this.request.getClientId(), this.request.getPublish(), this.request.getTimestamp(), this.request.getVectorClock()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.persistence));
    }

    public void onTimedOut() {
        LOGGER.trace("Message queue offer replicate request timed out");
        retryAndIncreaseTimeout(Void.class);
    }

    public void onBusy() {
        retry(Void.class);
    }

    public void onFailed() {
        retry(Void.class);
    }


    private static class Task
            implements Runnable {
        private final SettableFuture<Void> settableFuture;
        private final MessageQueueOfferReplicateRequest request;
        private final QueuedMessagesClusterPersistence persistence;

        Task(SettableFuture<Void> settableFuture,
             MessageQueueOfferReplicateRequest request,
             QueuedMessagesClusterPersistence persistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.persistence = persistence;
        }

        public void run() {
            this.settableFuture.setFuture(this.persistence.replicateOffer(
                    this.request.getClientId(), this.request.getPublish(),
                    this.request.getTimestamp(), this.request.getVectorClock()));
        }
    }
}
