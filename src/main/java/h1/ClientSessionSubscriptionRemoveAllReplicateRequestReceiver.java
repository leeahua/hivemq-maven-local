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
import z1.ClientSessionSubscriptionRemoveAllReplicateRequest;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

@CacheScoped
public class ClientSessionSubscriptionRemoveAllReplicateRequestReceiver
        extends GenericRequestReceiver
        implements ClusterReceiver<ClientSessionSubscriptionRemoveAllReplicateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionSubscriptionRemoveAllReplicateRequestReceiver.class);
    private final ClientSessionSubscriptionsClusterPersistence clientSessionSubscriptionsClusterPersistence;

    @Inject
    ClientSessionSubscriptionRemoveAllReplicateRequestReceiver(
            ClientSessionSubscriptionsClusterPersistence clientSessionSubscriptionsClusterPersistence,
            @Rpc ExecutorService rpcExecutor,
            @Primary ConsistentHashingRing primaryRing,
            @Minority ConsistentHashingRing minorityRing,
            ClusterIdProducer clusterIdProducer,
            ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer, clusterConfigurationService.getReplicates().getQueuedMessages().getReplicateCount(), rpcExecutor);
        this.clientSessionSubscriptionsClusterPersistence = clientSessionSubscriptionsClusterPersistence;
    }

    public void received(@NotNull ClientSessionSubscriptionRemoveAllReplicateRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received client session subscription REPLICATE REMOVE ALL request for client {}", request.getClientId());
        if (!writeable(request.getKey(), response, Void.class)) {
            return;
        }
        ListenableFuture<Void> future = this.clientSessionSubscriptionsClusterPersistence.removeAll(
                request.getClientId(), request.getVectorClock(), request.getTimestamp());
        sendResult(future, response);
    }

    protected void onFailure(Throwable t) {
        LOGGER.error("Exception while processing subscription remove all replication request.", t);
    }
}
