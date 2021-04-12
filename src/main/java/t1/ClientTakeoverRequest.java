package t1;

import j1.ClusterRequest;

public class ClientTakeoverRequest implements ClusterRequest {
    private final String clientId;

    public ClientTakeoverRequest(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }
}
