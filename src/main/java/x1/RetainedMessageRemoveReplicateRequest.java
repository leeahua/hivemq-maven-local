package x1;

import ak.VectorClock;
import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import j1.ClusterReplicateRequest;

public class RetainedMessageRemoveReplicateRequest extends ClusterReplicateRequest {
    private final String topic;

    public RetainedMessageRemoveReplicateRequest(
            long timestamp,
            @NotNull VectorClock vectorClock,
            @NotNull String topic) {
        super(timestamp, vectorClock);
        Preconditions.checkNotNull(vectorClock, "Vector clock must not be null");
        Preconditions.checkNotNull(topic, "Topic must not be null");
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }

    public String getKey() {
        return this.topic;
    }
}
