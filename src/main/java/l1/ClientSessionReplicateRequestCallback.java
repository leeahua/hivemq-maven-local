package l1;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import t1.ClientSessionReplicateRequest;
import v.ClientSessionClusterPersistence;

public class ClientSessionReplicateRequestCallback
        extends ClusterCallback<Void, ClientSessionReplicateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionReplicateRequestCallback.class);
    private final ClientSessionClusterPersistence persistence;

    public ClientSessionReplicateRequestCallback(
            ClientSessionClusterPersistence persistence) {
        this.persistence = persistence;
    }

    public void onSuccess(@Nullable Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(
                this.persistence.replicateClientSession(this.request.getClientId(), this.request.getClientSession(), this.request.getTimestamp(), this.request.getVectorClock()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(
                this.persistence.replicateClientSession(this.request.getClientId(), this.request.getClientSession(), this.request.getTimestamp(), this.request.getVectorClock()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.persistence));
    }

    public void onTimedOut() {
        this.LOGGER.trace("Client session replication request timed out.");
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
        private final ClientSessionReplicateRequest request;
        private final ClientSessionClusterPersistence persistence;

        Task(SettableFuture<Void> settableFuture,
             ClientSessionReplicateRequest request,
             ClientSessionClusterPersistence persistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.persistence = persistence;
        }

        public void run() {
            this.settableFuture.setFuture(this.persistence.replicateClientSession(
                    this.request.getClientId(), this.request.getClientSession(), this.request.getTimestamp(), this.request.getVectorClock()));
        }
    }
}
