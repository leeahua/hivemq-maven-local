package r1;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import y.ClientSessionSubscriptionsClusterPersistence;
import z1.ClientSessionSubscriptionRemoveReplicateRequest;

public class ClientSessionSubscriptionRemoveReplicateRequestCallback
        extends ClusterCallback<Void, ClientSessionSubscriptionRemoveReplicateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionSubscriptionRemoveReplicateRequestCallback.class);
    private final ClientSessionSubscriptionsClusterPersistence persistence;

    public ClientSessionSubscriptionRemoveReplicateRequestCallback(
            ClientSessionSubscriptionsClusterPersistence persistence) {
        this.persistence = persistence;
    }

    public void onSuccess(@Nullable Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(this.persistence.replicateRemove(this.request.getTimestamp(), this.request.getVectorClock(), this.request.getClientId(), this.request.getTopic()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.persistence.replicateRemove(this.request.getTimestamp(), this.request.getVectorClock(), this.request.getClientId(), this.request.getTopic()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.persistence));
    }

    public void onTimedOut() {
        this.LOGGER.trace("Subscription remove replication request timed out.");
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
        private final ClientSessionSubscriptionRemoveReplicateRequest request;
        private final ClientSessionSubscriptionsClusterPersistence persistence;

        Task(SettableFuture<Void> settableFuture,
             ClientSessionSubscriptionRemoveReplicateRequest request,
             ClientSessionSubscriptionsClusterPersistence persistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.persistence = persistence;
        }

        public void run() {
            this.settableFuture.setFuture(this.persistence.replicateRemove(this.request.getTimestamp(), this.request.getVectorClock(), this.request.getClientId(), this.request.getTopic()));
        }
    }
}
