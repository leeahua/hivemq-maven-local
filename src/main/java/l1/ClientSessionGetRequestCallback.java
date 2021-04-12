package l1;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import t1.ClientSessionGetRequest;
import v.ClientSession;
import v.ClientSessionClusterPersistence;

public class ClientSessionGetRequestCallback
        extends ClusterCallback<ClientSession, ClientSessionGetRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionGetRequestCallback.class);
    private final ClientSessionClusterPersistence persistence;

    public ClientSessionGetRequestCallback(ClientSessionClusterPersistence persistence) {
        this.persistence = persistence;
    }

    @Override
    public void onSuccess(@Nullable ClientSession result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(this.persistence.requestSession(this.request.getKey()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.persistence.requestSession(this.request.getKey()));
    }

    public void onSuspected() {
        retry(new Task(settableFuture, this.request, this.persistence));
    }

    public void onTimedOut() {
        this.LOGGER.trace("Client session get request timed out.");
        retryAndIncreaseTimeout(ClientSession.class);
    }

    public void onBusy() {
        retry(ClientSession.class);
    }

    private static class Task
            implements Runnable {
        private final SettableFuture<ClientSession> settableFuture;
        private final ClientSessionGetRequest request;
        private final ClientSessionClusterPersistence persistence;

        Task(SettableFuture<ClientSession> settableFuture,
             ClientSessionGetRequest request,
             ClientSessionClusterPersistence persistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.persistence = persistence;
        }

        public void run() {
            this.settableFuture.setFuture(this.persistence.requestSession(this.request.getKey()));
        }
    }
}
