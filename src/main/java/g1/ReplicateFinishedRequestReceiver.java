package g1;

import a1.ClusterReceiver;
import ab.ClusterResponse;
import ab.ClusterResponseCode;
import ah.ClusterService;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import y1.ReplicateFinishedRequest;

// TODO:
@CacheScoped
public class ReplicateFinishedRequestReceiver
        implements ClusterReceiver<ReplicateFinishedRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplicateFinishedRequestReceiver.class);
    private final ClusterService clusterService;

    @Inject
    ReplicateFinishedRequestReceiver(ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    public void received(@NotNull ReplicateFinishedRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        if (this.clusterService.d(sender)) {
            response.sendResult(ClusterResponseCode.FAILED);
            return;
        }
        boolean bool = !this.clusterService.c(sender);
        LOGGER.trace("Received replication finished request from {}. Response is {}.",
                sender, bool);
        response.sendResult(bool);
    }
}
