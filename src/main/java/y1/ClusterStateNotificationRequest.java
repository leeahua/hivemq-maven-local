package y1;

import ah.ClusterState;
import j1.ClusterRequest;

public class ClusterStateNotificationRequest implements ClusterRequest {
    private final String node;
    private final ClusterState state;
    
    public ClusterStateNotificationRequest(String node, ClusterState state) {
        this.node = node;
        this.state = state;
    }

    public String getNode() {
        return node;
    }

    public ClusterState getState() {
        return state;
    }
}
