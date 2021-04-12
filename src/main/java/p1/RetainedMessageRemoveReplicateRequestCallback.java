package p1;

import com.google.common.util.concurrent.SettableFuture;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import x.RetainedMessagesClusterPersistence;
import x1.RetainedMessageRemoveReplicateRequest;

public class RetainedMessageRemoveReplicateRequestCallback
        extends ClusterCallback<Void, RetainedMessageRemoveReplicateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetainedMessageRemoveReplicateRequestCallback.class);
    private final RetainedMessagesClusterPersistence persistence;

    public RetainedMessageRemoveReplicateRequestCallback(RetainedMessagesClusterPersistence persistence) {
        this.persistence = persistence;
    }

    public void onSuccess(Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(this.persistence.replicateRemove(this.request.getTimestamp(), this.request.getVectorClock(), this.request.getTopic()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.persistence.replicateRemove(this.request.getTimestamp(), this.request.getVectorClock(), this.request.getTopic()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.persistence));
    }

    public void onTimedOut() {
        LOGGER.info("Retained message remove replication request timeout.");
        retryAndIncreaseTimeout(Void.class);
    }

    public void onBusy() {
        retry(Void.class);
    }

    public void onFailed() {
        retry(Void.class);
    }


    private static class Task implements Runnable {
        private final SettableFuture<Void> settableFuture;
        private final RetainedMessageRemoveReplicateRequest request;
        private final RetainedMessagesClusterPersistence persistence;

        Task(SettableFuture<Void> settableFuture,
             RetainedMessageRemoveReplicateRequest request,
             RetainedMessagesClusterPersistence persistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.persistence = persistence;
        }

        public void run() {
            this.settableFuture.setFuture(this.persistence.replicateRemove(this.request.getTimestamp(), this.request.getVectorClock(), this.request.getTopic()));
        }
    }
}
