package i1;

import a1.ClusterReceiver;
import a1.GenericRequestReceiver;
import aa.TopicTreeRemoveRequest;
import ab.ClusterResponse;
import by.TopicTreeSinglePersistence;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
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
import s.Rpc;

import java.util.concurrent.ExecutorService;

@CacheScoped
public class TopicTreeRemoveRequestReceiver extends GenericRequestReceiver
        implements ClusterReceiver<TopicTreeRemoveRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicTreeAddRequestReceiver.class);
    private final TopicTreeSinglePersistence topicTreeSinglePersistence;

    @Inject
    TopicTreeRemoveRequestReceiver(TopicTreeSinglePersistence topicTreeSinglePersistence,
                                   @Rpc ExecutorService rpcExecutor,
                                   @Primary ConsistentHashingRing primaryRing,
                                   @Minority ConsistentHashingRing minorityRing,
                                   ClusterIdProducer clusterIdProducer,
                                   ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer,
                clusterConfigurationService.getReplicates().getQueuedMessages().getReplicateCount(), rpcExecutor);
        this.topicTreeSinglePersistence = topicTreeSinglePersistence;
    }

    public void received(@NotNull TopicTreeRemoveRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        this.LOGGER.trace("Received topic tree REMOVE request for topic {}", request.getTopic());
        if (!responsible(request.getKey(), response, Void.class)) {
            return;
        }
        ListenableFuture<Void> future = this.topicTreeSinglePersistence.removeSubscription(
                request.getSubscriber(), request.getTopic());
        sendResult(future, response);
    }

    protected void onFailure(Throwable t) {
        this.LOGGER.error("Exception while processing topic tree remove request.", t);
    }
}
