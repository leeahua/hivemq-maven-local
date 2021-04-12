package b1;

import a1.AbstractRequestReceiver;
import a1.ClusterReceiver;
import ab.ClusterResponse;
import com.google.common.base.Preconditions;
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
import t1.ClientSessionReplicateRequest;
import v.ClientSessionClusterPersistence;

@CacheScoped
public class ClientSessionReplicateRequestReceiver
        extends AbstractRequestReceiver
        implements ClusterReceiver<ClientSessionReplicateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionReplicateRequestReceiver.class);
    private final ClientSessionClusterPersistence clientSessionClusterPersistence;

    @Inject
    ClientSessionReplicateRequestReceiver(
            ClientSessionClusterPersistence clientSessionClusterPersistence,
            @Primary ConsistentHashingRing primaryRing,
            @Minority ConsistentHashingRing minorityRing,
            ClusterIdProducer clusterIdProducer,
            ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer, clusterConfigurationService.getReplicates().getClientSession().getReplicateCount());
        this.clientSessionClusterPersistence = clientSessionClusterPersistence;
    }

    public void received(@NotNull ClientSessionReplicateRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        this.LOGGER.trace("Received client session replication request for client {}.", request.getClientId());
        if (!writeable(request.getKey(), response, Void.class)) {
            return;
        }
        this.clientSessionClusterPersistence.handleReplica(request.getClientId(), request.getClientSession(), request.getTimestamp(), request.getVectorClock());
        response.sendResult();
    }
}
