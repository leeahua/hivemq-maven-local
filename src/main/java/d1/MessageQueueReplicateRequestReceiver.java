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
import v1.MessageQueueReplicateRequest;
import w.QueuedMessagesClusterPersistence;

import java.util.concurrent.ExecutorService;

@CacheScoped
public class MessageQueueReplicateRequestReceiver
        extends GenericRequestReceiver
        implements ClusterReceiver<MessageQueueReplicateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageQueueReplicateRequestReceiver.class);
    private final QueuedMessagesClusterPersistence queuedMessagesClusterPersistence;

    @Inject
    public MessageQueueReplicateRequestReceiver(
            QueuedMessagesClusterPersistence queuedMessagesClusterPersistence,
             @Rpc ExecutorService rpcExecutor,
             @Primary ConsistentHashingRing primaryRing,
             @Minority ConsistentHashingRing minorityRing,
             ClusterIdProducer clusterIdProducer,
             ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer, clusterConfigurationService.getReplicates().getQueuedMessages().getReplicateCount(), rpcExecutor);
        this.queuedMessagesClusterPersistence = queuedMessagesClusterPersistence;
    }

    public void received(@NotNull MessageQueueReplicateRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        this.LOGGER.trace("Received message queue replication request from node {}.", sender);
        ListenableFuture<Void> future = this.queuedMessagesClusterPersistence.processReplicationRequest(request);
        sendResult(future, response);
    }

    protected void onFailure(Throwable t) {
        this.LOGGER.error("Exception while processing message queue replication.", t);
    }
}
