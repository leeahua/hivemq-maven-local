package p1;

import com.google.common.util.concurrent.SettableFuture;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import x.RetainedMessagesSinglePersistence;
import x1.RetainedMessageAddRequest;

public class RetainedMessageAddRequestCallback
        extends ClusterCallback<Void, RetainedMessageAddRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetainedMessageAddRequestCallback.class);
    private final RetainedMessagesSinglePersistence persistence;

    public RetainedMessageAddRequestCallback(RetainedMessagesSinglePersistence persistence) {
        this.persistence = persistence;
    }

    public void onSuccess(Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(this.persistence.addOrReplace(this.request.getTopic(), this.request.getRetainedMessage()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.persistence.addOrReplace(this.request.getTopic(), this.request.getRetainedMessage()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.persistence));
    }

    public void onTimedOut() {
        LOGGER.trace("Retained message add request timeout.");
        retryAndIncreaseTimeout(Void.class);
    }

    public void onBusy() {
        retry(Void.class);
    }

    private static class Task implements Runnable {
        private final SettableFuture<Void> settableFuture;
        private final RetainedMessageAddRequest request;
        private final RetainedMessagesSinglePersistence persistence;

        Task(SettableFuture<Void> settableFuture,
             RetainedMessageAddRequest request,
             RetainedMessagesSinglePersistence persistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.persistence = persistence;
        }

        public void run() {
            this.settableFuture.setFuture(this.persistence.addOrReplace(this.request.getTopic(), this.request.getRetainedMessage()));
        }
    }
}
