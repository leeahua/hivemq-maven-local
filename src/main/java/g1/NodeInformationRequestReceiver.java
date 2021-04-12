package g1;

import a1.ClusterReceiver;
import ab.ClusterResponse;
import ah.ClusterContext;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import y1.NodeInformationRequest;

@CacheScoped
public class NodeInformationRequestReceiver
        implements ClusterReceiver<NodeInformationRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NodeInformationRequestReceiver.class);
    private final ClusterContext clusterContext;

    @Inject
    public NodeInformationRequestReceiver(ClusterContext clusterContext) {
        this.clusterContext = clusterContext;
    }

    public void received(@NotNull NodeInformationRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received cluster version request from {}.", sender);
        this.clusterContext.setVersion(request.getNode(), request.getVersion());
        response.sendResult();
    }
}
