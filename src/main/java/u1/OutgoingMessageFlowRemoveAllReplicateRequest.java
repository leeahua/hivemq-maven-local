package u1;

import ak.VectorClock;
import j1.ClusterKeyRequest;

public class OutgoingMessageFlowRemoveAllReplicateRequest
        implements ClusterKeyRequest {
    private final String clientId;
    private final VectorClock vectorClock;

    public OutgoingMessageFlowRemoveAllReplicateRequest(String clientId,
                                                        VectorClock vectorClock) {
        this.clientId = clientId;
        this.vectorClock = vectorClock;
    }

    public String getClientId() {
        return clientId;
    }

    public VectorClock getVectorClock() {
        return vectorClock;
    }

    public String getKey() {
        return this.clientId;
    }
}
