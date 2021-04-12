package y1;

import ah.ClusterState;
import j1.ClusterRequest;

public class ClusterStateRequest implements ClusterRequest {
    private final ClusterState state;

    public ClusterStateRequest(ClusterState state) {
        this.state = state;
    }

    public ClusterState getState() {
        return state;
    }
}
