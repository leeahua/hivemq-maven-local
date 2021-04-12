package h1;

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
import y.ClientSessionSubscriptionsClusterPersistence;
import z1.ClientSessionSubscriptionRemoveReplicateRequest;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

@CacheScoped
public class ClientSessionSubscriptionRemoveReplicateRequestReceiver
        extends GenericRequestReceiver
        implements ClusterReceiver<ClientSessionSubscriptionRemoveReplicateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionSubscriptionRemoveReplicateRequestReceiver.class);
    private final ClientSessionSubscriptionsClusterPersistence clientSessionSubscriptionsClusterPersistence;

    @Inject
    ClientSessionSubscriptionRemoveReplicateRequestReceiver(
            ClientSessionSubscriptionsClusterPersistence clientSessionSubscriptionsClusterPersistence,
            @Rpc ExecutorService rpcExecutor,
            @Primary ConsistentHashingRing primaryRing,
            @Minority ConsistentHashingRing minorityRing,
            ClusterIdProducer clusterIdProducer,
            ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer, clusterConfigurationService.getReplicates().getQueuedMessages().getReplicateCount(), rpcExecutor);
        this.clientSessionSubscriptionsClusterPersistence = clientSessionSubscriptionsClusterPersistence;
    }

    public void received(@NotNull ClientSessionSubscriptionRemoveReplicateRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received client session subscription REPLICATE REMOVE request for client {}", request.getClientId());
        if (!writeable(request.getKey(), response, Void.class)) {
            return;
        }
        ListenableFuture<Void> future = this.clientSessionSubscriptionsClusterPersistence.remove(
                request.getClientId(), request.getTopic(), request.getVectorClock(), request.getTimestamp());
        sendResult(future, response);
    }

    protected void onFailure(Throwable t) {
        LOGGER.error("Exception while processing subscription remove replication request.", t);
    }
}
