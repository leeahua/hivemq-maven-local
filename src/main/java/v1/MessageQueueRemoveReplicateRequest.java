package v1;

import ak.VectorClock;
import j1.ClusterReplicateRequest;

public class MessageQueueRemoveReplicateRequest extends ClusterReplicateRequest {
    private final String clientId;
    private final long entryTimestamp;
    private final String entryId;

    public MessageQueueRemoveReplicateRequest(String clientId,
                                              long entryTimestamp,
                                              String entryId,
                                              long timestamp,
                                              VectorClock vectorClock) {
        super(timestamp, vectorClock);
        this.clientId = clientId;
        this.entryTimestamp = entryTimestamp;
        this.entryId = entryId;
    }

    public String getClientId() {
        return clientId;
    }

    public long getEntryTimestamp() {
        return entryTimestamp;
    }

    public String getEntryId() {
        return entryId;
    }

    public String getKey() {
        return this.clientId;
    }
}
