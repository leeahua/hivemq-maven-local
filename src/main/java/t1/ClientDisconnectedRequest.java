package t1;

import j1.ClusterKeyRequest;

public class ClientDisconnectedRequest implements ClusterKeyRequest {
    private final String clientId;
    private final String connectedNode;

    public ClientDisconnectedRequest(String clientId, String connectedNode) {
        this.clientId = clientId;
        this.connectedNode = connectedNode;
    }

    public String getClientId() {
        return clientId;
    }

    public String getConnectedNode() {
        return connectedNode;
    }

    public String getKey() {
        return this.clientId;
    }
}
