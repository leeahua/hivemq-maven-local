package t1;

import j1.ClusterKeyRequest;

public class ClientSessionGetRequest implements ClusterKeyRequest {
    private final String clientId;

    public ClientSessionGetRequest(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getKey() {
        return this.clientId;
    }
}
