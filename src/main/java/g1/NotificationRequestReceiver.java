package g1;

import a1.ClusterReceiver;
import ab.ClusterResponse;
import ah.ClusterService;
import ah.ClusterState;
import ah.ClusterStateService;
import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import q.ConsistentHashingRing;
import s.Minority;
import s.Primary;
import t.ClusterConnection;
import y1.ClusterStateNotificationRequest;

import javax.inject.Inject;

@CacheScoped
public class NotificationRequestReceiver
        implements ClusterReceiver<ClusterStateNotificationRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationRequestReceiver.class);
    private final ClusterConnection clusterConnection;
    private final ClusterService clusterService;
    private final ConsistentHashingRing minorityRing;
    private final ConsistentHashingRing primaryRing;
    private final ClusterStateService clusterStateService;

    @Inject
    NotificationRequestReceiver(ClusterConnection clusterConnection,
                                ClusterService clusterService,
                                @Minority ConsistentHashingRing minorityRing,
                                @Primary ConsistentHashingRing primaryRing,
                                ClusterStateService clusterStateService) {
        this.clusterConnection = clusterConnection;
        this.clusterService = clusterService;
        this.minorityRing = minorityRing;
        this.primaryRing = primaryRing;
        this.clusterStateService = clusterStateService;
    }

    public void received(@NotNull ClusterStateNotificationRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        if (request.getNode().equals(this.clusterConnection.getClusterId())) {
            response.sendResult();
            return;
        }
        LOGGER.trace("Received {} notification, for {}, from {}.",
                request.getState(), request.getNode(), sender);
        if (request.getState() == ClusterState.MERGE_MINORITY) {
            this.minorityRing.remove(sender);
        }
        if (request.getState() == ClusterState.JOINING ||
                request.getState() == ClusterState.MERGING) {
            this.minorityRing.add(request.getNode());
            this.clusterService.joinMinority(request.getNode());
        }
        if (request.getState() == ClusterState.RUNNING &&
                (this.clusterStateService.getState(request.getNode()) == ClusterState.JOINING ||
                        this.clusterStateService.getState(request.getNode()) == ClusterState.MERGING)) {
            this.primaryRing.add(request.getNode());
            this.clusterService.removeReplication(request.getNode());
        }
        this.clusterStateService.change(request.getNode(), request.getState());
        response.sendResult();
    }
}
