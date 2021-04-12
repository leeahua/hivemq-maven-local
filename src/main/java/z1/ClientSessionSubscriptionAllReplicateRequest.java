package z1;

import com.google.common.collect.ImmutableSet;
import j1.ClusterRequest;

public class ClientSessionSubscriptionAllReplicateRequest
        implements ClusterRequest {
    private final ImmutableSet<ClientSessionSubscriptionReplicateRequest> requests;
    
    public ClientSessionSubscriptionAllReplicateRequest(ImmutableSet<ClientSessionSubscriptionReplicateRequest> requests) {
        this.requests = requests;
    }

    public ImmutableSet<ClientSessionSubscriptionReplicateRequest> getRequests() {
        return requests;
    }
}
