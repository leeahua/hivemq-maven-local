package v1;

import com.google.common.collect.ImmutableMap;
import j1.ClusterRequest;

public class MessageQueueReplicateRequest
        implements ClusterRequest {
    private final ImmutableMap<String, MessageQueueReplication> replications;

    public MessageQueueReplicateRequest(ImmutableMap<String, MessageQueueReplication> replications) {
        this.replications = replications;
    }

    public ImmutableMap<String, MessageQueueReplication> getReplications() {
        return replications;
    }
}
