package f1;

import a1.ClusterReceiver;
import a1.GenericRequestReceiver;
import ab.ClusterResponse;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
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
import x.RetainedMessagesClusterPersistence;
import x1.RetainedMessageAddReplicateRequest;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

@CacheScoped
public class RetainedMessageAddReplicateRequestReceiver
        extends GenericRequestReceiver
        implements ClusterReceiver<RetainedMessageAddReplicateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetainedMessageAddReplicateRequestReceiver.class);
    private final RetainedMessagesClusterPersistence retainedMessagesClusterPersistence;

    @Inject
    RetainedMessageAddReplicateRequestReceiver(
            RetainedMessagesClusterPersistence retainedMessagesClusterPersistence,
            @Rpc ExecutorService rpcExecutor,
            @Primary ConsistentHashingRing primaryRing,
            @Minority ConsistentHashingRing minorityRing,
            ClusterIdProducer clusterIdProducer,
            ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer, clusterConfigurationService.getReplicates().getQueuedMessages().getReplicateCount(), rpcExecutor);
        this.retainedMessagesClusterPersistence = retainedMessagesClusterPersistence;
    }

    public void received(@NotNull RetainedMessageAddReplicateRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received retained message REPLICATE ADD request for topic {}", request.getTopic());
        if (!writeable(request.getKey(), response, Void.class)) {
            return;
        }
        ListenableFuture<Void> future = this.retainedMessagesClusterPersistence.persist(
                request.getTopic(), request.getRetainedMessage(), request.getVectorClock(), request.getTimestamp());
        sendResult(future, response);
    }

    protected void onFailure(Throwable t) {
        LOGGER.error("Exception while processing retained message replicate add request.", t);
    }
}
