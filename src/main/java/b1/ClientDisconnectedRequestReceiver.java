package b1;

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
import s.Rpc;
import t1.ClientDisconnectedRequest;
import v.ClientSessionClusterPersistence;

import java.util.concurrent.ExecutorService;
import s.Primary;
import s.Minority;
import q.ConsistentHashingRing;

@CacheScoped
public class ClientDisconnectedRequestReceiver
        extends GenericRequestReceiver
        implements ClusterReceiver<ClientDisconnectedRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientDisconnectedRequestReceiver.class);
    private final ClientSessionClusterPersistence clientSessionClusterPersistence;

    @Inject
    ClientDisconnectedRequestReceiver(
            ClientSessionClusterPersistence clientSessionClusterPersistence,
            @Rpc ExecutorService rpcExecutor,
            @Primary ConsistentHashingRing primaryRing,
            @Minority ConsistentHashingRing minorityRing,
            ClusterIdProducer clusterIdProducer,
            ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer,
                clusterConfigurationService.getReplicates().getQueuedMessages().getReplicateCount(), rpcExecutor);
        this.clientSessionClusterPersistence = clientSessionClusterPersistence;
    }

    public void received(@NotNull ClientDisconnectedRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received client disconnected request for client {}.", request.getClientId());
        if (!responsible(request.getKey(), response, Void.class)) {
            return;
        }
        ListenableFuture<Void> future = this.clientSessionClusterPersistence.clientDisconnectedRequest(request.getClientId(), request.getConnectedNode());
        sendResult(future, response);
    }

    protected void onFailure(Throwable t) {
        LOGGER.error("Exception while processing client disconnected request.", t);
    }
}
