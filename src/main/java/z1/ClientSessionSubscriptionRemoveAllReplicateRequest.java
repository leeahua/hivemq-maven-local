package z1;

import ak.VectorClock;
import j1.ClusterReplicateRequest;

public class ClientSessionSubscriptionRemoveAllReplicateRequest
        extends ClusterReplicateRequest {
    private final String clientId;

    public ClientSessionSubscriptionRemoveAllReplicateRequest(long timestamp, VectorClock vectorClock, String clientId) {
        super(timestamp, vectorClock);
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getKey() {
        return this.clientId;
    }
}
