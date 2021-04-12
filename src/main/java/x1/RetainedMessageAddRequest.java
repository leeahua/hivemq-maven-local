package x1;

import bz.RetainedMessage;
import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import j1.ClusterTimestampRequest;


public class RetainedMessageAddRequest
        extends ClusterTimestampRequest {
    private final RetainedMessage retainedMessage;
    private final String topic;

    public RetainedMessageAddRequest(long timestamp,
                                     @NotNull String topic,
                                     @NotNull RetainedMessage retainedMessage) {
        super(timestamp);
        Preconditions.checkNotNull(topic, "Topic must not be null");
        Preconditions.checkNotNull(retainedMessage, "Retained message must not be null");
        this.retainedMessage = retainedMessage;
        this.topic = topic;
    }

    public RetainedMessage getRetainedMessage() {
        return retainedMessage;
    }

    public String getTopic() {
        return topic;
    }

    public String getKey() {
        return this.topic;
    }
}
