package r1;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import y.ClientSessionSubscriptionsClusterPersistence;
import z1.ClientSessionSubscriptionAddReplicateRequest;

public class ClientSessionSubscriptionAddReplicateRequestCallback
        extends ClusterCallback<Void, ClientSessionSubscriptionAddReplicateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionSubscriptionAddReplicateRequestCallback.class);
    private final ClientSessionSubscriptionsClusterPersistence persistence;

    public ClientSessionSubscriptionAddReplicateRequestCallback(
            ClientSessionSubscriptionsClusterPersistence persistence) {
        this.persistence = persistence;
    }

    public void onSuccess(@Nullable Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(this.persistence.replicateAdd(this.request.getTimestamp(), this.request.getVectorClock(), this.request.getClientId(), this.request.getTopic()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.persistence.replicateAdd(this.request.getTimestamp(), this.request.getVectorClock(), this.request.getClientId(), this.request.getTopic()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.persistence));
    }

    public void onTimedOut() {
        this.LOGGER.trace("Subscription add replication request timed out.");
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
        private final ClientSessionSubscriptionAddReplicateRequest request;
        private final ClientSessionSubscriptionsClusterPersistence persistence;

        public Task(SettableFuture<Void> settableFuture,
                    ClientSessionSubscriptionAddReplicateRequest request,
                    ClientSessionSubscriptionsClusterPersistence persistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.persistence = persistence;
        }

        public void run() {
            this.settableFuture.setFuture(this.persistence.replicateAdd(
                    this.request.getTimestamp(), this.request.getVectorClock(),
                    this.request.getClientId(), this.request.getTopic()));
        }
    }
}
