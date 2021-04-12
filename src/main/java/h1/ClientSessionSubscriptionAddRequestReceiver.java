package h1;

import a1.ClusterReceiver;
import a1.GenericRequestReceiver;
import ab.ClusterResponse;
import bm1.ClientSessionSubscriptionsSinglePersistence;
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
import z1.ClientSessionSubscriptionAddRequest;

import java.util.concurrent.ExecutorService;

@CacheScoped
public class ClientSessionSubscriptionAddRequestReceiver
        extends GenericRequestReceiver
        implements ClusterReceiver<ClientSessionSubscriptionAddRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionSubscriptionAddRequestReceiver.class);
    private final ClientSessionSubscriptionsSinglePersistence clientSessionSubscriptionsSinglePersistence;

    @Inject
    ClientSessionSubscriptionAddRequestReceiver(
            ClientSessionSubscriptionsSinglePersistence clientSessionSubscriptionsSinglePersistence,
            @Rpc ExecutorService rpcExecutor,
            @Primary ConsistentHashingRing primaryRing,
            @Minority ConsistentHashingRing minorityRing,
            ClusterIdProducer clusterIdProducer,
            ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer, clusterConfigurationService.getReplicates().getQueuedMessages().getReplicateCount(), rpcExecutor);
        this.clientSessionSubscriptionsSinglePersistence = clientSessionSubscriptionsSinglePersistence;
    }

    public void received(@NotNull ClientSessionSubscriptionAddRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received client session subscription ADD request for client {}", request.getClientId());
        if (!responsible(request.getKey(), response, Void.class)) {
            return;
        }
        ListenableFuture<Void> future = this.clientSessionSubscriptionsSinglePersistence.addSubscription(request.getClientId(), request.getTopic());
        sendResult(future, response);
    }

    protected void onFailure(Throwable t) {
        LOGGER.error("Exception while processing subscription add request.", t);
    }
}
