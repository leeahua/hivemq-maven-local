package u1;

import com.google.common.collect.ImmutableList;
import j1.ClusterRequest;

public class OutgoingMessageFlowReplicateRequest implements ClusterRequest {
    private final ImmutableList<OutgoingMessageFlowPutReplicateRequest> requests;

    public OutgoingMessageFlowReplicateRequest(ImmutableList<OutgoingMessageFlowPutReplicateRequest> requests) {
        this.requests = requests;
    }

    public ImmutableList<OutgoingMessageFlowPutReplicateRequest> getRequests() {
        return requests;
    }
}
