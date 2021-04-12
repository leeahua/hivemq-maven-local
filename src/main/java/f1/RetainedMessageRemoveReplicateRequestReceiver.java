package f1;

import a1.ClusterReceiver;
import a1.GenericRequestReceiver;
import ab.ClusterResponse;
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
import x.RetainedMessagesClusterPersistence;
import x1.RetainedMessageRemoveReplicateRequest;

import java.util.concurrent.ExecutorService;

@CacheScoped
public class RetainedMessageRemoveReplicateRequestReceiver
        extends GenericRequestReceiver
        implements ClusterReceiver<RetainedMessageRemoveReplicateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetainedMessageRemoveReplicateRequestReceiver.class);
    private final RetainedMessagesClusterPersistence retainedMessagesClusterPersistence;

    @Inject
    RetainedMessageRemoveReplicateRequestReceiver(
            RetainedMessagesClusterPersistence retainedMessagesClusterPersistence,
            @Rpc ExecutorService rpcExecutor,
            @Primary ConsistentHashingRing primaryRing,
            @Minority ConsistentHashingRing minorityRing,
            ClusterIdProducer clusterIdProducer,
            ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer, clusterConfigurationService.getReplicates().getQueuedMessages().getReplicateCount(), rpcExecutor);
        this.retainedMessagesClusterPersistence = retainedMessagesClusterPersistence;
    }

    public void received(@NotNull RetainedMessageRemoveReplicateRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(request, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received retained message REPLICATE REMOVE request for topic {}", request.getTopic());
        if (!writeable(request.getKey(), response, Void.class)) {
            return;
        }
        ListenableFuture<Void> future = this.retainedMessagesClusterPersistence.remove(request.getTopic(), request.getVectorClock(), request.getTimestamp());
        sendResult(future, response);
    }

    protected void onFailure(Throwable t) {
        LOGGER.error("Exception while processing retained message replicate remove request.", t);
    }
}
