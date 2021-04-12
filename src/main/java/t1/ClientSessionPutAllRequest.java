package t1;

import com.google.common.collect.ImmutableSet;
import j1.ClusterRequest;

public class ClientSessionPutAllRequest implements ClusterRequest {
    private final ImmutableSet<ClientSessionReplicateRequest> requests;

    public ClientSessionPutAllRequest(ImmutableSet<ClientSessionReplicateRequest> requests) {
        this.requests = requests;
    }

    public ImmutableSet<ClientSessionReplicateRequest> getRequests() {
        return requests;
    }
}
