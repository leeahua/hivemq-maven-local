package h1;

import a1.ClusterReceiver;
import a1.GenericRequestReceiver;
import ab.ClusterResponse;
import aj.ClusterFutures;
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
import y.ClientSessionSubscriptionsClusterPersistence;
import z1.ClientSessionSubscriptionAllReplicateRequest;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@CacheScoped
public class ClientSessionSubscriptionAllReplicateRequestReceiver
        extends GenericRequestReceiver
        implements ClusterReceiver<ClientSessionSubscriptionAllReplicateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionSubscriptionAllReplicateRequestReceiver.class);
    private final ClientSessionSubscriptionsClusterPersistence clientSessionSubscriptionsClusterPersistence;

    @Inject
    ClientSessionSubscriptionAllReplicateRequestReceiver(
            ClientSessionSubscriptionsClusterPersistence clientSessionSubscriptionsClusterPersistence,
            @Rpc ExecutorService rpcExecutor,
            @Primary ConsistentHashingRing primaryRing,
            @Minority ConsistentHashingRing minorityRing,
            ClusterIdProducer clusterIdProducer,
            ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer, clusterConfigurationService.getReplicates().getQueuedMessages().getReplicateCount(), rpcExecutor);
        this.clientSessionSubscriptionsClusterPersistence = clientSessionSubscriptionsClusterPersistence;
    }

    public void received(@NotNull ClientSessionSubscriptionAllReplicateRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        this.LOGGER.trace("Received client session subscription REPLICATE ALL request.");
        List<ListenableFuture<Void>> futures = request.getRequests().stream()
                .map(r->this.clientSessionSubscriptionsClusterPersistence.handleReplica(
                        r.getClientId(), r.getSubscriptions(), r.getVectorClock(), r.getTimestamp()))
                .collect(Collectors.toList());
        sendResult(ClusterFutures.merge(futures), response);
    }

    protected void onFailure(Throwable t) {
        this.LOGGER.error("Exception while processing subscription replicate all request.", t);
    }
}
