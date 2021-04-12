package i1;

import a1.AbstractRequestReceiver;
import a1.ClusterReceiver;
import aa.TopicTreeGetRequest;
import ab.ClusterResponse;
import ai.TopicSubscribers;
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
public class TopicTreeGetRequestReceiver extends AbstractRequestReceiver
        implements ClusterReceiver<TopicTreeGetRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicTreeGetRequestReceiver.class);
    private final TopicTreeClusterPersistence topicTreeClusterPersistence;

    @Inject
    TopicTreeGetRequestReceiver(
            TopicTreeClusterPersistence topicTreeClusterPersistence,
            @Primary ConsistentHashingRing primaryRing,
            @Minority ConsistentHashingRing minorityRing,
            ClusterIdProducer clusterIdProducer,
            ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer, clusterConfigurationService.getReplicates().getClientSession().getReplicateCount());
        this.topicTreeClusterPersistence = topicTreeClusterPersistence;
    }

    public void received(@NotNull TopicTreeGetRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        this.LOGGER.trace("Received topic tree GET request for topic.", request.getTopic());
        if (!readable(request.getKey(), response, TopicSubscribers.class)) {
            return;
        }
        TopicSubscribers topicSubscribers = this.topicTreeClusterPersistence.getLocally(request.getSegmentKey(), request.getTopic());
        response.sendResult(topicSubscribers);
    }
}
