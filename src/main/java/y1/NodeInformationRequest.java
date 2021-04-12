package y1;

import j1.ClusterRequest;

public class NodeInformationRequest implements ClusterRequest {
    private final String node;
    private final String version;

    public NodeInformationRequest(String node, String version) {
        this.node = node;
        this.version = version;
    }

    public String getNode() {
        return node;
    }

    public String getVersion() {
        return version;
    }
}
