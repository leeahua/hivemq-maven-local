package l1;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import t1.ClientConnectedRequest;
import u1.MessageFlow;
import v.ClientSessionClusterPersistence;

public class ClientConnectedRequestCallback
        extends ClusterCallback<MessageFlow, ClientConnectedRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientConnectedRequestCallback.class);
    private final ClientSessionClusterPersistence persistence;

    public ClientConnectedRequestCallback(ClientSessionClusterPersistence persistence) {
        this.persistence = persistence;
    }

    public void onSuccess(@Nullable MessageFlow result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(
                this.persistence.clientConnectedRequest(this.request.getClientId(),
                        this.request.isPersistentSession(), this.request.getConnectedNode()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(
                this.persistence.clientConnectedRequest(this.request.getClientId(),
                        this.request.isPersistentSession(), this.request.getConnectedNode()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.persistence));
    }

    public void onTimedOut() {
        this.LOGGER.trace("Client connected request timed out.");
        retryAndIncreaseTimeout(MessageFlow.class);
    }

    public void onBusy() {
        retry(MessageFlow.class);
    }

    private static class Task implements Runnable {
        private final SettableFuture<MessageFlow> settableFuture;
        private final ClientConnectedRequest request;
        private final ClientSessionClusterPersistence persistence;

        Task(SettableFuture<MessageFlow> settableFuture,
             ClientConnectedRequest request,
             ClientSessionClusterPersistence persistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.persistence = persistence;
        }

        public void run() {
            this.settableFuture.setFuture(
                    this.persistence.clientConnectedRequest(this.request.getClientId(),
                            this.request.isPersistentSession(), this.request.getConnectedNode()));
        }
    }
}
