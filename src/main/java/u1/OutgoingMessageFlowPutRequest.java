package u1;

import com.google.common.collect.ImmutableList;
import com.hivemq.spi.message.MessageWithId;
import j1.ClusterKeyRequest;


public class OutgoingMessageFlowPutRequest
        implements ClusterKeyRequest {
    private final String clientId;
    private final ImmutableList<MessageWithId> messages;

    public OutgoingMessageFlowPutRequest(String clientId,
                                         ImmutableList<MessageWithId> messages) {
        this.clientId = clientId;
        this.messages = messages;
    }

    public String getClientId() {
        return clientId;
    }

    public ImmutableList<MessageWithId> getMessages() {
        return messages;
    }

    public String getKey() {
        return this.clientId;
    }
}
