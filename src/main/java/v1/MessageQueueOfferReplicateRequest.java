package v1;

import ak.VectorClock;
import bu.InternalPublish;
import j1.ClusterReplicateRequest;

public class MessageQueueOfferReplicateRequest extends ClusterReplicateRequest {
    private final String clientId;
    private final InternalPublish publish;

    public MessageQueueOfferReplicateRequest(String clientId,
                                             long timestamp,
                                             VectorClock vectorClock,
                                             InternalPublish publish) {
        super(timestamp, vectorClock);
        this.clientId = clientId;
        this.publish = publish;
    }

    public String getClientId() {
        return clientId;
    }

    public InternalPublish getPublish() {
        return publish;
    }

    public String getKey() {
        return this.clientId;
    }
}
