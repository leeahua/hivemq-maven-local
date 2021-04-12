package r1;

import bm1.ClientSessionSubscriptionsSinglePersistence;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import z1.ClientSessionSubscriptionAddRequest;

public class ClientSessionSubscriptionAddRequestCallback
        extends ClusterCallback<Void, ClientSessionSubscriptionAddRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionSubscriptionAddRequestCallback.class);
    private final ClientSessionSubscriptionsSinglePersistence persistence;

    public ClientSessionSubscriptionAddRequestCallback(
            ClientSessionSubscriptionsSinglePersistence persistence) {
        this.persistence = persistence;
    }

    public void onSuccess(@Nullable Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(this.persistence.addSubscription(this.request.getClientId(), this.request.getTopic()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.persistence.addSubscription(this.request.getClientId(), this.request.getTopic()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.persistence));
    }

    public void onTimedOut() {
        this.LOGGER.trace("Subscription add request timed out.");
        retryAndIncreaseTimeout(Void.class);
    }

    public void onBusy() {
        retry(Void.class);
    }

    private static class Task
            implements Runnable {
        private final SettableFuture<Void> settableFuture;
        private final ClientSessionSubscriptionAddRequest request;
        private final ClientSessionSubscriptionsSinglePersistence persistence;

        Task(SettableFuture<Void> settableFuture,
             ClientSessionSubscriptionAddRequest request,
             ClientSessionSubscriptionsSinglePersistence persistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.persistence = persistence;
        }

        public void run() {
            this.settableFuture.setFuture(this.persistence.addSubscription(
                    this.request.getClientId(), this.request.getTopic()));
        }
    }
}
