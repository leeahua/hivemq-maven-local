package x1;

import ak.VectorClock;
import bz.RetainedMessage;
import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import j1.ClusterReplicateRequest;

public class RetainedMessageAddReplicateRequest extends ClusterReplicateRequest {
    private final String topic;
    private final RetainedMessage retainedMessage;
    
    public RetainedMessageAddReplicateRequest(long timestamp,
                                              @NotNull VectorClock vectorClock,
                                              @NotNull String topic,
                                              @NotNull RetainedMessage retainedMessage) {
        super(timestamp, vectorClock);
        Preconditions.checkNotNull(vectorClock, "Vector clock must not be null");
        Preconditions.checkNotNull(topic, "Topic must not be null");
        Preconditions.checkNotNull(retainedMessage, "Retained message must not be null");
        this.topic = topic;
        this.retainedMessage = retainedMessage;
    }

    public String getTopic() {
        return topic;
    }

    public RetainedMessage getRetainedMessage() {
        return retainedMessage;
    }

    public String getKey() {
        return this.topic;
    }
}
