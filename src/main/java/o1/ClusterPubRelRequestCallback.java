package o1;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import i.PublishDispatcher;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import p.ClusterPublishDeniedException;
import w1.ClusterPubRelRequest;

public class ClusterPubRelRequestCallback
        extends ClusterCallback<Void, ClusterPubRelRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterPubRelRequestCallback.class);
    private final PublishDispatcher publishDispatcher;

    public ClusterPubRelRequestCallback(PublishDispatcher publishDispatcher) {
        this.publishDispatcher = publishDispatcher;
    }

    public void onSuccess(@Nullable Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setException(new ClusterPublishDeniedException());
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.publishDispatcher.dispatch(this.request.getPubRel(), this.request.getClientId()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.publishDispatcher));
    }

    public void onFailure(Throwable t) {
        this.LOGGER.warn("Exception while sending publish request.", t);
        retry(new Task(this.settableFuture, this.request, this.publishDispatcher));
    }

    public void onTimedOut() {
        this.LOGGER.trace("Publish request timed out.");
        retryAndIncreaseTimeout(Void.class);
    }

    public void onBusy() {
        retry(Void.class);
    }


    private static class Task
            implements Runnable {
        private final SettableFuture<Void> settableFuture;
        private final ClusterPubRelRequest request;
        private final PublishDispatcher publishDispatcher;

        Task(SettableFuture<Void> settableFuture,
             ClusterPubRelRequest request,
             PublishDispatcher publishDispatcher) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.publishDispatcher = publishDispatcher;
        }

        public void run() {
            this.settableFuture.setFuture(
                    this.publishDispatcher.dispatch(this.request.getPubRel(), this.request.getClientId()));
        }
    }
}
