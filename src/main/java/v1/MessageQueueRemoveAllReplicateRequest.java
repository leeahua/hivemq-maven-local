package v1;

import ak.VectorClock;
import j1.ClusterReplicateRequest;

public class MessageQueueRemoveAllReplicateRequest
        extends ClusterReplicateRequest {
    private final String clientId;

    public MessageQueueRemoveAllReplicateRequest(String clientId,
                                                 long timestamp,
                                                 VectorClock vectorClock) {
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
