package s1;

import aa.TopicTreeAddRequest;
import by.TopicTreeSinglePersistence;
import com.google.common.util.concurrent.SettableFuture;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TopicTreeAddRequestCallback
        extends ClusterCallback<Void, TopicTreeAddRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicTreeAddRequestCallback.class);
    private final TopicTreeSinglePersistence topicTreeSinglePersistence;

    public TopicTreeAddRequestCallback(TopicTreeSinglePersistence topicTreeSinglePersistence) {
        this.topicTreeSinglePersistence = topicTreeSinglePersistence;
    }

    public void onSuccess(Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(this.topicTreeSinglePersistence.addSubscription(this.request.getSubscriber(), this.request.getTopic(), this.request.getShared(), this.request.getGroupId()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.topicTreeSinglePersistence.addSubscription(this.request.getSubscriber(), this.request.getTopic(), this.request.getShared(), this.request.getGroupId()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.topicTreeSinglePersistence));
    }

    public void onTimedOut() {
        LOGGER.trace("Topic Tree ADD request timeout.");
        retryAndIncreaseTimeout(Void.class);
    }

    public void onBusy() {
        retry(Void.class);
    }


    private static class Task
            implements Runnable {
        private final SettableFuture<Void> settableFuture;
        private final TopicTreeAddRequest request;
        private final TopicTreeSinglePersistence topicTreeSinglePersistence;

        Task(SettableFuture<Void> settableFuture,
             TopicTreeAddRequest request,
             TopicTreeSinglePersistence topicTreeSinglePersistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.topicTreeSinglePersistence = topicTreeSinglePersistence;
        }

        public void run() {
            this.settableFuture.setFuture(this.topicTreeSinglePersistence.addSubscription(
                    this.request.getSubscriber(), this.request.getTopic(), this.request.getShared(), this.request.getGroupId()));
        }
    }
}
