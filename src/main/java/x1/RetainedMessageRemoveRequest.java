package x1;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import j1.ClusterTimestampRequest;

public class RetainedMessageRemoveRequest extends ClusterTimestampRequest {
    private final String topic;
    
    public RetainedMessageRemoveRequest(long timestamp, @NotNull String topic) {
        super(timestamp);
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
