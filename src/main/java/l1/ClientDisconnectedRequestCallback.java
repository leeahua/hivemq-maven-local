package l1;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import t1.ClientDisconnectedRequest;
import v.ClientSessionClusterPersistence;

public class ClientDisconnectedRequestCallback
        extends ClusterCallback<Void, ClientDisconnectedRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientDisconnectedRequestCallback.class);
    private final ClientSessionClusterPersistence persistence;

    public ClientDisconnectedRequestCallback(ClientSessionClusterPersistence persistence) {
        this.persistence = persistence;
    }

    public void onSuccess(@Nullable Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(this.persistence.clientDisconnectedRequest(
                this.request.getClientId(), this.request.getConnectedNode()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.persistence.clientDisconnectedRequest(
                this.request.getClientId(), this.request.getConnectedNode()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.persistence));
    }

    public void onTimedOut() {
        LOGGER.trace("Client disconnected request timed out.");
        retryAndIncreaseTimeout(Void.class);
    }

    public void onBusy() {
        retry(Void.class);
    }


    private static class Task implements Runnable {
        private final SettableFuture<Void> settableFuture;
        private final ClientDisconnectedRequest request;
        private final ClientSessionClusterPersistence persistence;

        Task(SettableFuture<Void> settableFuture,
             ClientDisconnectedRequest request,
             ClientSessionClusterPersistence persistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.persistence = persistence;
        }

        public void run() {
            this.settableFuture.setFuture(this.persistence.clientDisconnectedRequest(
                    this.request.getClientId(), this.request.getConnectedNode()));
        }
    }
}
