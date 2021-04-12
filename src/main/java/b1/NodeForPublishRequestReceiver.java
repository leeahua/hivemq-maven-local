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
import q.ConsistentHashingRing;
import s.Minority;
import s.Primary;
import t1.NodeForPublishRequest;
import v.ClientSessionClusterPersistence;

import javax.annotation.Nullable;

@CacheScoped
public class NodeForPublishRequestReceiver
        extends AbstractRequestReceiver
        implements ClusterReceiver<NodeForPublishRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NodeForPublishRequestReceiver.class);
    private final ClientSessionClusterPersistence clientSessionClusterPersistence;

    @Inject
    public NodeForPublishRequestReceiver(
            ClientSessionClusterPersistence clientSessionClusterPersistence,
            @Primary ConsistentHashingRing primaryRing,
            @Minority ConsistentHashingRing minorityRing,
            ClusterIdProducer clusterIdProducer,
            ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer, clusterConfigurationService.getReplicates().getClientSession().getReplicateCount());
        this.clientSessionClusterPersistence = clientSessionClusterPersistence;
    }

    public void received(@NotNull NodeForPublishRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Receiver node for publish request.");
        if (!responsible(request.getKey(), response, String.class)) {
            return;
        }
        ListenableFuture<String> future = this.clientSessionClusterPersistence.handleNodeForPublishRequest(request.getClientId());
        ClusterFutures.addCallback(future, new FutureCallback<String>(){

            @Override
            public void onSuccess(@Nullable String result) {
                response.sendResult(request);
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.error("Exception a NodeForPublishRequest", t);
                response.sendResult(ClusterResponseCode.DENIED);
            }
        });
    }
}
