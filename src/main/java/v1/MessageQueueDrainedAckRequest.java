package v1;

import j1.ClusterRequest;

public class MessageQueueDrainedAckRequest implements ClusterRequest {
    private final String clientId;

    public MessageQueueDrainedAckRequest(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }
}
