package z1;

import j1.ClusterKeyRequest;

public class ClientSessionSubscriptionGetRequest implements ClusterKeyRequest {
    private final String clientId;

    public ClientSessionSubscriptionGetRequest(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getKey() {
        return this.clientId;
    }
}
