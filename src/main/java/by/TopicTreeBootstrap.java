package by;

import aj.ClusterFutures;
import bx.SharedTopicUtils;
import co.SharedSubscriptionServiceImpl;
import co.SharedSubscriptionServiceImpl.SharedTopic;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.message.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import v.ClientSessionClusterPersistence;
import y.ClientSessionSubscriptionsClusterPersistence;
import y.ClientSubscriptions;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Set;

public class TopicTreeBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicTreeBootstrap.class);
    private final TopicTreeClusterPersistence topicTreeClusterPersistence;
    private final ClientSessionClusterPersistence clientSessionClusterPersistence;
    private final ClientSessionSubscriptionsClusterPersistence clientSessionSubscriptionsClusterPersistence;
    private final SharedSubscriptionServiceImpl sharedSubscriptionService;

    @Inject
    TopicTreeBootstrap(TopicTreeClusterPersistence topicTreeClusterPersistence,
                       ClientSessionClusterPersistence clientSessionClusterPersistence,
                       ClientSessionSubscriptionsClusterPersistence clientSessionSubscriptionsClusterPersistence,
                       SharedSubscriptionServiceImpl sharedSubscriptionService) {
        this.topicTreeClusterPersistence = topicTreeClusterPersistence;
        this.clientSessionClusterPersistence = clientSessionClusterPersistence;
        this.clientSessionSubscriptionsClusterPersistence = clientSessionSubscriptionsClusterPersistence;
        this.sharedSubscriptionService = sharedSubscriptionService;
    }

    @PostConstruct
    void init() {
        LOGGER.debug("Building initial topic tree");
        initTopicTree();
    }

    private void initTopicTree() {
        ListenableFuture<Set<String>> future = this.clientSessionClusterPersistence.getLocalAllClients();
        ClusterFutures.addCallback(future, new FutureCallback<Set<String>>(){

            @Override
            public void onSuccess(@Nullable Set<String> result) {
                result.forEach(clientId->{
                    ClientSubscriptions clientSubscriptions = clientSessionSubscriptionsClusterPersistence.getClientSubscriptions(clientId);
                    clientSubscriptions.getSubscriptions().forEach(topic -> {
                        SharedTopic sharedTopic = sharedSubscriptionService.getSharedTopic(topic.getTopic());
                        if (sharedTopic == null) {
                            topicTreeClusterPersistence.subscribe(clientId, topic, SharedTopicUtils.getShared(false), null);
                        } else {
                            topicTreeClusterPersistence.subscribe(clientId,
                                    new Topic(sharedTopic.getTopic(), topic.getQoS()),
                                    SharedTopicUtils.getShared(true), sharedTopic.getGroupId());
                        }
                    });
                });
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.error("Failed to bootstrap topic tree.", t);
            }
        });
    }
}
