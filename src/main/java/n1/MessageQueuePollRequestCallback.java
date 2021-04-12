package n1;

import bm1.QueuedMessagesSinglePersistence;
import bu.InternalPublish;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import v1.MessageQueuePollRequest;
import w.QueuedMessagesClusterPersistence;

public class MessageQueuePollRequestCallback
        extends ClusterCallback<InternalPublish, MessageQueuePollRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageQueuePollRequestCallback.class);
    private final QueuedMessagesSinglePersistence singlePersistence;
    private final QueuedMessagesClusterPersistence clusterPersistence;

    public MessageQueuePollRequestCallback(
            QueuedMessagesSinglePersistence singlePersistence,
            QueuedMessagesClusterPersistence clusterPersistence) {
        this.singlePersistence = singlePersistence;
        this.clusterPersistence = clusterPersistence;
    }

    public void onSuccess(@Nullable InternalPublish result) {
        if (result == null) {
            this.clusterPersistence.ackDrained(this.request.getClientId());
        }
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(this.singlePersistence.poll(this.request.getClientId()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.singlePersistence.poll(this.request.getClientId()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.singlePersistence));
    }

    public void onTimedOut() {
        LOGGER.trace("Message queue poll request timeout.");
        retryAndIncreaseTimeout(InternalPublish.class);
    }

    public void onBusy() {
        retry(InternalPublish.class);
    }


    private static class Task
            implements Runnable {
        private final SettableFuture<InternalPublish> settableFuture;
        private final MessageQueuePollRequest request;
        private final QueuedMessagesSinglePersistence singlePersistence;

        Task(SettableFuture<InternalPublish> settableFuture,
             MessageQueuePollRequest request,
             QueuedMessagesSinglePersistence singlePersistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.singlePersistence = singlePersistence;
        }

        public void run() {
            this.settableFuture.setFuture(this.singlePersistence.poll(this.request.getClientId()));
        }
    }
}
