package z1;

import com.hivemq.spi.message.Topic;
import j1.ClusterTimestampRequest;

public class ClientSessionSubscriptionAddRequest
        extends ClusterTimestampRequest {
    private final String clientId;
    private final Topic topic;

    public ClientSessionSubscriptionAddRequest(long timestamp, String clientId, Topic topic) {
        super(timestamp);
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
