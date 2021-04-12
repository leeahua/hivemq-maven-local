package y1;

import com.google.common.collect.ImmutableMap;
import j1.ClusterRequest;

public class ClusterContextRequest implements ClusterRequest {
    private final ImmutableMap<String, String> nodeInformations;

    public ClusterContextRequest(ImmutableMap<String, String> nodeInformations) {
        this.nodeInformations = nodeInformations;
    }

    public ImmutableMap<String, String> getNodeInformations() {
        return nodeInformations;
    }
}
