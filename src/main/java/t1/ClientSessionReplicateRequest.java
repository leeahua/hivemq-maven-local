package t1;

import ak.VectorClock;
import j1.ClusterReplicateRequest;
import v.ClientSession;

public class ClientSessionReplicateRequest extends ClusterReplicateRequest {
    private final String clientId;
    private final ClientSession clientSession;

    public ClientSessionReplicateRequest(long timestamp,
                                         VectorClock vectorClock,
                                         String clientId,
                                         ClientSession clientSession) {
        super(timestamp, vectorClock);
        this.clientId = clientId;
        this.clientSession = clientSession;
    }

    public String getClientId() {
        return clientId;
    }

    public ClientSession getClientSession() {
        return clientSession;
    }

    public String getKey() {
        return this.clientId;
    }
}
