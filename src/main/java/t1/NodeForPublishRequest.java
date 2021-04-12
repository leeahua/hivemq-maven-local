package t1;

import j1.ClusterKeyRequest;

public class NodeForPublishRequest implements ClusterKeyRequest {
    private final String clientId;
    
    public NodeForPublishRequest(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getKey() {
        return this.clientId;
    }
}
