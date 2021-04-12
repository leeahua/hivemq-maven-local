package s1;

import aa.TopicTreeRemoveRequest;
import ai.TopicTreeClusterPersistenceImpl;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicTreeRemoveRequestCallback
        extends ClusterCallback<Void, TopicTreeRemoveRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicTreeRemoveRequestCallback.class);
    private final TopicTreeClusterPersistenceImpl persistence;

    public TopicTreeRemoveRequestCallback(TopicTreeClusterPersistenceImpl persistence) {
        this.persistence = persistence;
    }

    public void onSuccess(@Nullable Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(this.persistence.removeSubscription(this.request.getSubscriber(), this.request.getTopic()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.persistence.removeSubscription(this.request.getSubscriber(), this.request.getTopic()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.persistence));
    }

    public void onTimedOut() {
        LOGGER.trace("Topic Tree Remove request timeout.");
        retryAndIncreaseTimeout(Void.class);
    }

    public void onBusy() {
        retry(Void.class);
    }


    private static class Task implements Runnable {
        private final SettableFuture<Void> settableFuture;
        private final TopicTreeRemoveRequest request;
        private final TopicTreeClusterPersistenceImpl persistence;

        Task(SettableFuture<Void> settableFuture,
             TopicTreeRemoveRequest request,
             TopicTreeClusterPersistenceImpl persistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.persistence = persistence;
        }

        public void run() {
            this.settableFuture.setFuture(this.persistence.removeSubscription(this.request.getSubscriber(), this.request.getTopic()));
        }
    }
}
