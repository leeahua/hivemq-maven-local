package b1;

import a1.AbstractRequestReceiver;
import a1.ClusterReceiver;
import ab.ClusterResponse;
import ab.ClusterResponseCode;
import aj.ClusterFutures;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import i.ClusterConfigurationService;
import i.ClusterIdProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import t1.ClientConnectedRequest;
import u1.MessageFlow;
import v.ClientSessionClusterPersistence;
import s.Primary;
import s.Minority;
import q.ConsistentHashingRing;

import javax.annotation.Nullable;

@CacheScoped
public class ClientConnectedRequestReceiver
        extends AbstractRequestReceiver
        implements ClusterReceiver<ClientConnectedRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientConnectedRequestReceiver.class);
    private final ClientSessionClusterPersistence clientSessionClusterPersistence;

    @Inject
    ClientConnectedRequestReceiver(ClientSessionClusterPersistence clientSessionClusterPersistence,
                                   @Primary ConsistentHashingRing primaryRing,
                                   @Minority ConsistentHashingRing minorityRing,
                                   ClusterIdProducer clusterIdProducer,
                                   ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer, clusterConfigurationService.getReplicates().getClientSession().getReplicateCount());
        this.clientSessionClusterPersistence = clientSessionClusterPersistence;
    }

    public void received(@NotNull ClientConnectedRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received client connected request for client {}.", request.getClientId());
        if (!responsible(request.getKey(), response, MessageFlow.class)) {
            return;
        }
        ListenableFuture<MessageFlow> future = this.clientSessionClusterPersistence.clientConnectedRequest(
                request.getClientId(), request.isPersistentSession(), request.getConnectedNode());
        ClusterFutures.addCallback(future, new FutureCallback<MessageFlow>() {

            @Override
            public void onSuccess(@Nullable MessageFlow result) {
                response.sendResult(result);
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.error("Exception while processing client connected request.", t);
                response.sendResult(ClusterResponseCode.FAILED);
            }
        });
    }
}
