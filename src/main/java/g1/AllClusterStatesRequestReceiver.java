package g1;

import a1.ClusterReceiver;
import ab.ClusterResponse;
import ah.ClusterStateService;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import y1.AllClusterStatesRequest;

@CacheScoped
public class AllClusterStatesRequestReceiver
        implements ClusterReceiver<AllClusterStatesRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AllClusterStatesRequestReceiver.class);
    private final ClusterStateService clusterStateService;
    
    @Inject
    public AllClusterStatesRequestReceiver(ClusterStateService clusterStateService) {
        this.clusterStateService = clusterStateService;
    }

    public void received(@NotNull AllClusterStatesRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received all cluster states request form {}.", sender);
        response.sendResult(this.clusterStateService.getClusterNodeStates());
    }
}
