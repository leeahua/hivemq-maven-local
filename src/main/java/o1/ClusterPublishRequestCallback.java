package o1;

import bo.SendStatus;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import i.PublishDispatcherImpl;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import p.ClusterPublishDeniedException;
import w1.ClusterPublishRequest;

public class ClusterPublishRequestCallback
        extends ClusterCallback<SendStatus, ClusterPublishRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterPublishRequestCallback.class);
    private final PublishDispatcherImpl publishDispatcher;

    public ClusterPublishRequestCallback(PublishDispatcherImpl publishDispatcher) {
        this.publishDispatcher = publishDispatcher;
    }

    public void onSuccess(@Nullable SendStatus result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setException(new ClusterPublishDeniedException());
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.publishDispatcher.b(this.request.getPublish(), this.request.getClientId(), this.request.getQoSNumber(), this.request.d(), this.request.e(), this.request.f()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.publishDispatcher));
    }

    public void onTimedOut() {
        LOGGER.trace("Publish request timed out.");
        retryAndIncreaseTimeout(SendStatus.class);
    }

    public void onBusy() {
        retry(SendStatus.class);
    }

    private static class Task
            implements Runnable {
        private final SettableFuture<SendStatus> settableFuture;
        private final ClusterPublishRequest request;
        private final PublishDispatcherImpl publishDispatcher;

        Task(SettableFuture<SendStatus> settableFuture,
             ClusterPublishRequest request,
             PublishDispatcherImpl publishDispatcher) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.publishDispatcher = publishDispatcher;
        }

        public void run() {
            this.settableFuture.setFuture(this.publishDispatcher.b(this.request.getPublish(),
                    this.request.getClientId(), this.request.getQoSNumber(), this.request.d(),
                    this.request.e(), this.request.f()));
        }
    }
}
