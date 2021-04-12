package h1;

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
import y.ClientSessionSubscriptionsClusterPersistence;
import y.ClientSubscriptions;
import z1.ClientSessionSubscriptionGetRequest;

@CacheScoped
public class ClientSessionSubscriptionGetRequestReceiver
        extends AbstractRequestReceiver
        implements ClusterReceiver<ClientSessionSubscriptionGetRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionSubscriptionGetRequestReceiver.class);
    private final ClientSessionSubscriptionsClusterPersistence clientSessionSubscriptionsClusterPersistence;

    @Inject
    ClientSessionSubscriptionGetRequestReceiver(
            ClientSessionSubscriptionsClusterPersistence clientSessionSubscriptionsClusterPersistence,
            @Primary ConsistentHashingRing primaryRing,
            @Minority ConsistentHashingRing minorityRing,
            ClusterIdProducer clusterIdProducer,
            ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer, clusterConfigurationService.getReplicates().getClientSession().getReplicateCount());
        this.clientSessionSubscriptionsClusterPersistence = clientSessionSubscriptionsClusterPersistence;
    }

    public void received(@NotNull ClientSessionSubscriptionGetRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received client session subscription GET request for client {}", request.getClientId());
        if (!readable(request.getKey(), response, ClientSubscriptions.class)) {
            return;
        }
        response.sendResult(this.clientSessionSubscriptionsClusterPersistence.getClientSubscriptions(request.getClientId()));
    }
}
