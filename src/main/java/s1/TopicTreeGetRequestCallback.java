package s1;

import aa.TopicTreeGetRequest;
import ai.TopicSubscribers;
import ai.TopicTreeClusterPersistenceImpl;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicTreeGetRequestCallback
        extends ClusterCallback<TopicSubscribers, TopicTreeGetRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicTreeGetRequestCallback.class);
    private final TopicTreeClusterPersistenceImpl persistence;

    public TopicTreeGetRequestCallback(TopicTreeClusterPersistenceImpl persistence) {
        this.persistence = persistence;
    }

    public void onSuccess(@Nullable TopicSubscribers result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(this.persistence.getSubscribers(this.request.getTopic()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.persistence.getSubscribers(this.request.getTopic()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.persistence));
    }

    public void onTimedOut() {
        this.LOGGER.trace("Topic tree get request timed out.");
        retryAndIncreaseTimeout(TopicSubscribers.class);
    }

    public void onBusy() {
        retry(TopicSubscribers.class);
    }


    private static class Task
            implements Runnable {
        private final SettableFuture<TopicSubscribers> settableFuture;
        private final TopicTreeGetRequest request;
        private final TopicTreeClusterPersistenceImpl persistence;

        Task(SettableFuture<TopicSubscribers> settableFuture,
             TopicTreeGetRequest request, TopicTreeClusterPersistenceImpl persistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.persistence = persistence;
        }

        public void run() {
            this.settableFuture.setFuture(this.persistence.getSubscribers(this.request.getTopic()));
        }
    }
}
