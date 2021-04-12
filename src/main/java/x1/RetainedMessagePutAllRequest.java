package x1;

import com.google.common.collect.ImmutableSet;
import j1.ClusterRequest;
import j1.ClusterReplicateRequest;

public class RetainedMessagePutAllRequest implements ClusterRequest {
    private final ImmutableSet<ClusterReplicateRequest> requests;

    public RetainedMessagePutAllRequest(ImmutableSet<ClusterReplicateRequest> requests) {
        this.requests = requests;
    }


    public ImmutableSet<ClusterReplicateRequest> getRequests() {
        return requests;
    }
}
