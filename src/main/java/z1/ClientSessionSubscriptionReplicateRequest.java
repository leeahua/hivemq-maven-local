package z1;

import ak.VectorClock;
import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.message.Topic;
import j1.ClusterReplicateRequest;

public class ClientSessionSubscriptionReplicateRequest extends ClusterReplicateRequest {
    private final String clientId;
    private final ImmutableSet<Topic> subscriptions;

    public ClientSessionSubscriptionReplicateRequest(long timestamp,
                                                     VectorClock vectorClock,
                                                     String clientId,
                                                     ImmutableSet<Topic> paramImmutableSet) {
        super(timestamp, vectorClock);
        this.clientId = clientId;
        this.subscriptions = paramImmutableSet;
    }

    public String getClientId() {
        return clientId;
    }

    public ImmutableSet<Topic> getSubscriptions() {
        return subscriptions;
    }

    public String getKey() {
        return this.clientId;
    }
}
