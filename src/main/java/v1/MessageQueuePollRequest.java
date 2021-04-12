package v1;

import j1.ClusterKeyRequest;

import java.util.UUID;

public class MessageQueuePollRequest implements ClusterKeyRequest {
    private final String clientId;
    private final String requestId;
    
    public MessageQueuePollRequest(String clientId) {
        this.clientId = clientId;
        this.requestId = UUID.randomUUID().toString();
    }

    public MessageQueuePollRequest(String clientId, String requestId) {
        this.clientId = clientId;
        this.requestId = requestId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getKey() {
        return this.clientId;
    }
}
