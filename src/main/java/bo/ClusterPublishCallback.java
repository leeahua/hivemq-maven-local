package bo;

import aj.ClusterFutures;
import am1.Metrics;
import bu.InternalPublish;
import bx.SubscriberWithQoS;
import co.PublishServiceImpl;
import co.SharedSubscriptionServiceImpl;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import i.ClusterIdProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ClusterPublishCallback implements FutureCallback<ClusterPublishResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterPublishCallback.class);
    private final ArrayList<SubscriberWithQoS> publishedSubscribers;
    private final List<SubscriberWithQoS> groupSubscribers;
    private final InternalPublish publish;
    private final SubscriberWithQoS subscriber;
    private final SharedSubscriptionServiceImpl sharedSubscriptionService;
    private final PublishServiceImpl publishService;
    private final Metrics metrics;
    private final ClusterIdProducer clusterIdProducer;
    private final ExecutorService executorService;

    public ClusterPublishCallback(ArrayList<SubscriberWithQoS> publishedSubscribers,
                                  List<SubscriberWithQoS> groupSubscribers,
                                  InternalPublish publish,
                                  SubscriberWithQoS subscriber,
                                  SharedSubscriptionServiceImpl sharedSubscriptionService,
                                  PublishServiceImpl publishService,
                                  Metrics metrics,
                                  ClusterIdProducer clusterIdProducer,
                                  ExecutorService executorService) {
        this.publishedSubscribers = publishedSubscribers;
        this.groupSubscribers = groupSubscribers;
        this.publish = publish;
        this.subscriber = subscriber;
        this.sharedSubscriptionService = sharedSubscriptionService;
        this.publishService = publishService;
        this.metrics = metrics;
        this.clusterIdProducer = clusterIdProducer;
        this.executorService = executorService;
    }

    @Override
    public void onSuccess(@javax.annotation.Nullable ClusterPublishResult result) {
        if (result == null) {
            onCompleted(SendStatus.FAILED);
            return;
        }
        if (result.getStatus() == null) {
            onCompleted(SendStatus.FAILED);
            return;
        }
        switch (result.getStatus()) {
            case DELIVERED:
                if (this.clusterIdProducer.get().equals(result.getClientId())) {
                    this.sharedSubscriptionService.addSubscriberToGroup(this.subscriber, this.subscriber.getGroupId());
                } else {
                    this.sharedSubscriptionService.removeSubscriberFromGroup(this.subscriber, this.subscriber.getGroupId());
                }
                break;
            case NOT_CONNECTED:
                break;
            case MESSAGE_IDS_EXHAUSTED:
                if (this.clusterIdProducer.get().equals(result.getClientId())) {
                    this.sharedSubscriptionService.removeSubscriberFromGroup(this.subscriber, this.subscriber.getGroupId());
                }
                this.groupSubscribers.remove(this.subscriber);
                this.publishedSubscribers.remove(this.subscriber);
                onCompleted(result.getStatus());
                break;
            case QUEUED:
                this.groupSubscribers.remove(this.subscriber);
                this.publishedSubscribers.remove(this.subscriber);
                onCompleted(result.getStatus());
                break;
            case FAILED:
                this.groupSubscribers.remove(this.subscriber);
                this.publishedSubscribers.remove(this.subscriber);
                onCompleted(result.getStatus());
                break;
            case IN_PROGRESS:
                this.publishedSubscribers.add(this.subscriber);
                onCompleted(result.getStatus());
        }
    }


    private void onCompleted(SendStatus sendStatus) {
        if (!this.groupSubscribers.isEmpty() &&
                this.publishedSubscribers.size() == this.groupSubscribers.size()) {
            SubscriberWithQoS subscribers = this.publishedSubscribers.get(0);
            this.publishedSubscribers.remove(0);
            this.groupSubscribers.remove(subscribers);
            ListenableFuture<ClusterPublishResult>  publishFuture = this.publishService.a(this.publish, subscribers.getSubscriber(), subscribers.getQosNumber(), false, this.executorService);
            ClusterFutures.addCallback(publishFuture, new ClusterPublishCallback(this.publishedSubscribers, this.groupSubscribers, this.publish, subscribers, this.sharedSubscriptionService, this.publishService, this.metrics, this.clusterIdProducer, this.executorService));
            return;
        }
        if (this.groupSubscribers.isEmpty()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Not able to deliver message '{}' to shared subscription group '{}', reason: {}",
                        this.publish.getUniqueId(), this.subscriber.getGroupId(), sendStatus);
            }
            this.metrics.droppedMessageRate().mark();
            this.metrics.droppedMessageCount().inc();
            return;
        }
        SubscriberWithQoS nextSubscriber = this.sharedSubscriptionService.nextSubscriber(this.publishedSubscribers, this.groupSubscribers, this.subscriber.getGroupId());
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("retrying shared subscription for message '{}' with subscriber '{}' for group '{}', reason: {}",
                    this.publish.getUniqueId(), nextSubscriber, nextSubscriber.getGroupId(), sendStatus);
        }
        ListenableFuture<ClusterPublishResult>  publishFuture =
                this.publishService.a(this.publish, nextSubscriber.getSubscriber(),
                        nextSubscriber.getQosNumber(), true, this.executorService);
        ClusterFutures.addCallback(publishFuture,
                new ClusterPublishCallback(this.publishedSubscribers, this.groupSubscribers, this.publish, nextSubscriber,
                        this.sharedSubscriptionService, this.publishService, this.metrics,
                        this.clusterIdProducer, this.executorService));
    }


    public void onFailure(Throwable t) {
        LOGGER.error("Not able to send out message to shared subscriber '{}' in group '{}'",
                this.subscriber.getSubscriber(), this.subscriber.getGroupId(), t);
    }
}
