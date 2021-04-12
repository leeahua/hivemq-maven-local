package s1;

import aa.TopicTreeRemoveReplicateRequest;
import ai.TopicTreeClusterPersistenceImpl;
import com.google.common.util.concurrent.SettableFuture;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicTreeRemoveReplicateRequestCallback
        extends ClusterCallback<Void, TopicTreeRemoveReplicateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicTreeRemoveReplicateRequestCallback.class);
    private final TopicTreeClusterPersistenceImpl persistence;

    public TopicTreeRemoveReplicateRequestCallback(TopicTreeClusterPersistenceImpl persistence) {
        this.persistence = persistence;
    }

    public void onSuccess(Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(this.persistence.replicateRemove(this.request.getSubscriber(), this.request.getTopic(), this.request.getSegmentKey()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.persistence.replicateRemove(this.request.getSubscriber(), this.request.getTopic(), this.request.getSegmentKey()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.persistence));
    }

    public void onTimedOut() {
        LOGGER.trace("Topic Tree Remove replication request timeout.");
        retryAndIncreaseTimeout(Void.class);
    }

    public void onBusy() {
        retry(Void.class);
    }

    public void onFailed() {
        retry(Void.class);
    }

    private static class Task implements Runnable {
        private final SettableFuture<Void> settableFuture;
        private final TopicTreeRemoveReplicateRequest request;
        private final TopicTreeClusterPersistenceImpl persistence;

        Task(SettableFuture<Void> settableFuture,
             TopicTreeRemoveReplicateRequest request,
             TopicTreeClusterPersistenceImpl persistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.persistence = persistence;
        }

        public void run() {
            this.settableFuture.setFuture(this.persistence.replicateRemove(this.request.getSubscriber(), this.request.getTopic(), this.request.getSegmentKey()));
        }
    }
}
