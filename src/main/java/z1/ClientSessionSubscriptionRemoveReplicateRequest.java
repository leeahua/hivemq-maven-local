package z1;

import ak.VectorClock;
import com.hivemq.spi.message.Topic;
import j1.ClusterReplicateRequest;

public class ClientSessionSubscriptionRemoveReplicateRequest
        extends ClusterReplicateRequest {
    private final String clientId;
    private final Topic topic;

    public ClientSessionSubscriptionRemoveReplicateRequest(long timestamp, VectorClock vectorClock, String clientId, Topic topic) {
        super(timestamp, vectorClock);
        this.clientId = clientId;
        this.topic = topic;
    }

    public String getClientId() {
        return clientId;
    }

    public Topic getTopic() {
        return topic;
    }

    public String getKey() {
        return this.clientId;
    }
}
