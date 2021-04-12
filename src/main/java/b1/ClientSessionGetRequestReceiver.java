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
import s.Primary;
import t1.ClientSessionGetRequest;
import v.ClientSession;
import v.ClientSessionClusterPersistence;

import javax.annotation.Nullable;

import s.Minority;
import q.ConsistentHashingRing;

@CacheScoped
public class ClientSessionGetRequestReceiver
        extends AbstractRequestReceiver
        implements ClusterReceiver<ClientSessionGetRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionGetRequestReceiver.class);
    private final ClientSessionClusterPersistence clientSessionClusterPersistence;

    @Inject
    ClientSessionGetRequestReceiver(ClientSessionClusterPersistence clientSessionClusterPersistence,
                                    @Primary ConsistentHashingRing primaryRing,
                                    @Minority ConsistentHashingRing minorityRing,
                                    ClusterIdProducer clusterIdProducer,
                                    ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer,
                clusterConfigurationService.getReplicates()
                        .getClientSession().getReplicateCount());
        this.clientSessionClusterPersistence = clientSessionClusterPersistence;
    }

    public void received(@NotNull ClientSessionGetRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received client session get request for client {}.", request.getClientId());
        if (!readable(request.getKey(), response, ClientSession.class)) {
            return;
        }
        ListenableFuture<ClientSession> future = this.clientSessionClusterPersistence.getLocally(request.getClientId());
        ClusterFutures.addCallback(future, new FutureCallback<ClientSession>() {
            @Override
            public void onSuccess(@Nullable ClientSession result) {
                response.sendResult(result);
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.error("Exception at ClientSessionGetRequest", t);
                response.sendResult(ClusterResponseCode.DENIED);
            }
        });
    }
}
