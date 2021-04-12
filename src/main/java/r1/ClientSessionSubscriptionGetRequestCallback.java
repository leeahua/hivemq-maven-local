package r1;

import bm1.ClientSessionSubscriptionsSinglePersistence;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import y.ClientSubscriptions;
import z1.ClientSessionSubscriptionGetRequest;

public class ClientSessionSubscriptionGetRequestCallback
        extends ClusterCallback<ClientSubscriptions, ClientSessionSubscriptionGetRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionSubscriptionGetRequestCallback.class);
    private final ClientSessionSubscriptionsSinglePersistence persistence;

    public ClientSessionSubscriptionGetRequestCallback(
            ClientSessionSubscriptionsSinglePersistence persistence) {
        this.persistence = persistence;
    }

    public void onSuccess(@Nullable ClientSubscriptions result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(this.persistence.getSubscriptions(this.request.getClientId()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.persistence.getSubscriptions(this.request.getClientId()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.persistence));
    }

    public void onTimedOut() {
        this.LOGGER.trace("Subscription get request timed out.");
        retryAndIncreaseTimeout(ClientSubscriptions.class);
    }

    public void onBusy() {
        retry(ClientSubscriptions.class);
    }

    private static class Task
            implements Runnable {
        private final SettableFuture<ClientSubscriptions> settableFuture;
        private final ClientSessionSubscriptionGetRequest request;
        private final ClientSessionSubscriptionsSinglePersistence persistence;

        Task(SettableFuture<ClientSubscriptions> settableFuture,
             ClientSessionSubscriptionGetRequest request,
             ClientSessionSubscriptionsSinglePersistence persistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.persistence = persistence;
        }

        public void run() {
            this.settableFuture.setFuture(
                    this.persistence.getSubscriptions(this.request.getClientId()));
        }
    }
}
