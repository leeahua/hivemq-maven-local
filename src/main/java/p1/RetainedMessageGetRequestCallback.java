package p1;

import bz.RetainedMessage;
import com.google.common.util.concurrent.SettableFuture;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import x.RetainedMessagesSinglePersistence;
import x1.RetainedMessageGetRequest;

public class RetainedMessageGetRequestCallback
        extends ClusterCallback<RetainedMessage, RetainedMessageGetRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetainedMessageGetRequestCallback.class);
    private final RetainedMessagesSinglePersistence persistence;

    public RetainedMessageGetRequestCallback(RetainedMessagesSinglePersistence persistence) {
        this.persistence = persistence;
    }

    public void onSuccess(RetainedMessage result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(this.persistence.getWithoutWildcards(this.request.getTopic()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.persistence.getWithoutWildcards(this.request.getTopic()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.persistence));
    }

    public void onTimedOut() {
        LOGGER.info("Retained message get request timeout.");
        retryAndIncreaseTimeout(RetainedMessage.class);
    }

    public void onBusy() {
        retry(RetainedMessage.class);
    }

    private static class Task
            implements Runnable {
        private final SettableFuture<RetainedMessage> settableFuture;
        private final RetainedMessageGetRequest request;
        private final RetainedMessagesSinglePersistence persistence;

        Task(SettableFuture<RetainedMessage> settableFuture,
             RetainedMessageGetRequest request,
             RetainedMessagesSinglePersistence persistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.persistence = persistence;
        }

        public void run() {
            this.settableFuture.setFuture(this.persistence.getWithoutWildcards(this.request.getTopic()));
        }
    }
}
