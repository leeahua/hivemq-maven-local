package i1;

import a1.ClusterReceiver;
import aa.TopicTreeReplicateSegmentRequest;
import ab.ClusterResponse;
import ac.SerializationService;
import by.TopicTreeClusterPersistence;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CacheScoped
public class TopicTreeReplicateSegmentRequestReceiver
        implements ClusterReceiver<TopicTreeReplicateSegmentRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicTreeReplicateSegmentRequestReceiver.class);
    final SerializationService serializationService;
    final TopicTreeClusterPersistence topicTreeClusterPersistence;

    @Inject
    TopicTreeReplicateSegmentRequestReceiver(SerializationService serializationService, TopicTreeClusterPersistence topicTreeClusterPersistence) {
        this.serializationService = serializationService;
        this.topicTreeClusterPersistence = topicTreeClusterPersistence;
    }

    public void received(@NotNull TopicTreeReplicateSegmentRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received topic tree REPLICATE SEGMENTS request.");
        request.getSegments().forEach(segment ->
                this.topicTreeClusterPersistence.mergeSegment(segment, segment.getKey())
        );
        response.sendResult();
    }
}
