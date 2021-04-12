package r1;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import y.ClientSessionSubscriptionsClusterPersistence;
import z1.ClientSessionSubscriptionRemoveAllReplicateRequest;

public class ClientSessionSubscriptionRemoveAllReplicateRequestCallback
        extends ClusterCallback<Void, ClientSessionSubscriptionRemoveAllReplicateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionSubscriptionRemoveAllReplicateRequestCallback.class);
    private final ClientSessionSubscriptionsClusterPersistence persistence;

    public ClientSessionSubscriptionRemoveAllReplicateRequestCallback(
            ClientSessionSubscriptionsClusterPersistence persistence) {
        this.persistence = persistence;
    }

    public void onSuccess(@Nullable Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(this.persistence.replicateRemoveAll(this.request.getTimestamp(), this.request.getVectorClock(), this.request.getClientId()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.persistence.replicateRemoveAll(this.request.getTimestamp(), this.request.getVectorClock(), this.request.getClientId()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.persistence));
    }

    public void onTimedOut() {
        this.LOGGER.trace("Subscription remove all replication request timed out.");
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
        private final ClientSessionSubscriptionRemoveAllReplicateRequest request;
        private final ClientSessionSubscriptionsClusterPersistence persistence;

        Task(SettableFuture<Void> settableFuture,
             ClientSessionSubscriptionRemoveAllReplicateRequest request,
             ClientSessionSubscriptionsClusterPersistence persistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.persistence = persistence;
        }

        public void run() {
            this.settableFuture.setFuture(this.persistence.replicateRemoveAll(
                    this.request.getTimestamp(), this.request.getVectorClock(), this.request.getClientId()));
        }
    }
}
