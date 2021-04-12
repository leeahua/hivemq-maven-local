package i1;

import a1.AbstractRequestReceiver;
import a1.ClusterReceiver;
import aa.TopicTreeRemoveReplicateRequest;
import ab.ClusterResponse;
import by.TopicTreeClusterPersistence;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import i.ClusterConfigurationService;
import i.ClusterIdProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import q.ConsistentHashingRing;
import s.Minority;
import s.Primary;

@CacheScoped
public class TopicTreeRemoveReplicateRequestReceiver
        extends AbstractRequestReceiver
        implements ClusterReceiver<TopicTreeRemoveReplicateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicTreeReplicateAddRequestReceiver.class);
    private final TopicTreeClusterPersistence topicTreeClusterPersistence;

    @Inject
    TopicTreeRemoveReplicateRequestReceiver(
            TopicTreeClusterPersistence topicTreeClusterPersistence,
            @Primary ConsistentHashingRing primaryRing,
            @Minority ConsistentHashingRing minorityRing,
            ClusterIdProducer clusterIdProducer,
            ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer, clusterConfigurationService.getReplicates().getClientSession().getReplicateCount());
        this.topicTreeClusterPersistence = topicTreeClusterPersistence;
    }

    public void received(@NotNull TopicTreeRemoveReplicateRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received topic tree REPLICATE REMOVE request for topic {}", request.getTopic());
        boolean isWildcardTopic = request.getTopic().startsWith("#") ||
                request.getTopic().startsWith("+");
        if (!isWildcardTopic && !writeable(request.getKey(), response, Void.class)) {
            return;
        }
        this.topicTreeClusterPersistence.removeTopicReplica(request.getSubscriber(), request.getTopic(), request.getSegmentKey());
        response.sendResult();
    }
}
