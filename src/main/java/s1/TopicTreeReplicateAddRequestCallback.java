package s1;

import aa.TopicTreeReplicateAddRequest;
import ai.TopicTreeClusterPersistenceImpl;
import com.google.common.util.concurrent.SettableFuture;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicTreeReplicateAddRequestCallback
        extends ClusterCallback<Void, TopicTreeReplicateAddRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicTreeReplicateAddRequestCallback.class);
    private final TopicTreeClusterPersistenceImpl topicTreePersistence;

    public TopicTreeReplicateAddRequestCallback(TopicTreeClusterPersistenceImpl topicTreePersistence) {
        this.topicTreePersistence = topicTreePersistence;
    }

    public void onSuccess(Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(this.topicTreePersistence.replicateAdd(this.request.getSubscriber(), this.request.getTopic(), this.request.getSegmentKey(), this.request.getShared(), this.request.getGroupId()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.topicTreePersistence.replicateAdd(this.request.getSubscriber(), this.request.getTopic(), this.request.getSegmentKey(), this.request.getShared(), this.request.getGroupId()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.topicTreePersistence));
    }

    public void onTimedOut() {
        LOGGER.trace("Topic Tree Add replication request timeout.");
        retryAndIncreaseTimeout(Void.class);
    }

    public void onBusy() {
        retry(Void.class);
    }

    public void onFailed() {
        retry(Void.class);
    }


    private static class Task
            implements Runnable {
        private final SettableFuture<Void> settableFuture;
        private final TopicTreeReplicateAddRequest request;
        private final TopicTreeClusterPersistenceImpl topicTreeClusterPersistence;

        Task(SettableFuture<Void> settableFuture,
             TopicTreeReplicateAddRequest request,
             TopicTreeClusterPersistenceImpl topicTreeClusterPersistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.topicTreeClusterPersistence = topicTreeClusterPersistence;
        }

        public void run() {
            this.settableFuture.setFuture(this.topicTreeClusterPersistence.replicateAdd(
                    this.request.getSubscriber(), this.request.getTopic(), this.request.getSegmentKey(), this.request.getShared(), this.request.getGroupId()));
        }
    }
}
