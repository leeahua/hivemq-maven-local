package t1;

import j1.ClusterRequest;

public class ClientDataRequest implements ClusterRequest {
    private final String clientId;

    public ClientDataRequest(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }
}
