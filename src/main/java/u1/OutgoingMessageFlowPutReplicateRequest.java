package u1;

import ak.VectorClock;
import com.google.common.collect.ImmutableList;
import com.hivemq.spi.message.MessageWithId;
import j1.ClusterKeyRequest;

public class OutgoingMessageFlowPutReplicateRequest implements ClusterKeyRequest {
    private final String clientId;
    private final ImmutableList<MessageWithId> messages;
    private final VectorClock vectorClock;

    public OutgoingMessageFlowPutReplicateRequest(
            String clientId,
            ImmutableList<MessageWithId> messages,
            VectorClock vectorClock) {
        this.clientId = clientId;
        this.messages = messages;
        this.vectorClock = vectorClock;
    }

    public String getClientId() {
        return clientId;
    }

    public ImmutableList<MessageWithId> getMessages() {
        return messages;
    }

    public VectorClock getVectorClock() {
        return vectorClock;
    }

    public String getKey() {
        return this.clientId;
    }
}
