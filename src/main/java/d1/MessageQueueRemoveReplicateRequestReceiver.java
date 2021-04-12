package d1;

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
import v1.MessageQueueRemoveReplicateRequest;
import w.QueuedMessagesClusterPersistence;

import java.util.concurrent.ExecutorService;

@CacheScoped
public class MessageQueueRemoveReplicateRequestReceiver
        extends GenericRequestReceiver
        implements ClusterReceiver<MessageQueueRemoveReplicateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageQueueRemoveReplicateRequestReceiver.class);
    private final QueuedMessagesClusterPersistence queuedMessagesClusterPersistence;

    @Inject
    public MessageQueueRemoveReplicateRequestReceiver(
            QueuedMessagesClusterPersistence queuedMessagesClusterPersistence,
            @Rpc ExecutorService rpcExecutor,
            @Primary ConsistentHashingRing primaryRing,
            @Minority ConsistentHashingRing minorityRing,
            ClusterIdProducer clusterIdProducer,
            ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer, clusterConfigurationService.getReplicates().getQueuedMessages().getReplicateCount(), rpcExecutor);
        this.queuedMessagesClusterPersistence = queuedMessagesClusterPersistence;
    }

    public void received(@NotNull MessageQueueRemoveReplicateRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received message queue remove replication for client {}.", request.getClientId());
        if (!writeable(request.getKey(), response, Void.class)) {
            return;
        }
        ListenableFuture<Void> future = this.queuedMessagesClusterPersistence.processRemoveReplica(
                request.getClientId(), request.getEntryTimestamp(), request.getEntryId(), request.getVectorClock(), request.getTimestamp());
        sendResult(future, response);
    }

    protected void onFailure(Throwable t) {
        LOGGER.error("Exception while procession message queue remove replication", t);
    }
}
