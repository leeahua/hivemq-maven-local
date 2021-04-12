package c1;

import a1.ClusterReceiver;
import a1.GenericRequestReceiver;
import ab.ClusterResponse;
import bn1.OutgoingMessageFlowClusterPersistence;
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
import u1.OutgoingMessageFlowPutReplicateRequest;

import java.util.concurrent.ExecutorService;

@CacheScoped
public class OutgoingMessageFlowPutReplicateRequestReceiver
        extends GenericRequestReceiver
        implements ClusterReceiver<OutgoingMessageFlowPutReplicateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutgoingMessageFlowPutReplicateRequestReceiver.class);
    private final OutgoingMessageFlowClusterPersistence outgoingMessageFlowClusterPersistence;

    @Inject
    public OutgoingMessageFlowPutReplicateRequestReceiver(
            OutgoingMessageFlowClusterPersistence outgoingMessageFlowClusterPersistence,
            @Rpc ExecutorService rpcExecutor,
            @Primary ConsistentHashingRing primaryRing,
            @Minority ConsistentHashingRing minorityRing,
            ClusterIdProducer clusterIdProducer,
            ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer, clusterConfigurationService.getReplicates().getQueuedMessages().getReplicateCount(), rpcExecutor);
        this.outgoingMessageFlowClusterPersistence = outgoingMessageFlowClusterPersistence;
    }

    public void received(@NotNull OutgoingMessageFlowPutReplicateRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received outgoing message flow put request for client {}.", request.getClientId());
        if (!writeable(request.getKey(), response, Void.class)) {
            return;
        }
        ListenableFuture<Void> future = this.outgoingMessageFlowClusterPersistence.handlePutReplica(request);
        sendResult(future, response);
    }

    protected void onFailure(Throwable t) {
        LOGGER.error("Exception while responding to outgoing message flow put replica request.", t);
    }
}
